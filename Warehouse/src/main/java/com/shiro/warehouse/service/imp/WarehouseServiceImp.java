package com.shiro.warehouse.service.imp;

import static com.shiro.warehouse.dto.WarehouseDtos.Status.*;

import com.shiro.warehouse.client.FlashSaleClient;
import com.shiro.warehouse.config.AppProperties;
import com.shiro.warehouse.constants.PurchaseStatus;
import com.shiro.warehouse.constants.ServiceConstants;
import com.shiro.warehouse.dto.PurchaseStatusSyncDtos;
import com.shiro.warehouse.dto.WarehouseDtos;
import com.shiro.warehouse.entity.InventoryReservation;
import com.shiro.warehouse.repository.InventoryRepository;
import com.shiro.warehouse.repository.InventoryReservationRepository;
import com.shiro.warehouse.service.WarehouseService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.kafka.common.errors.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Manages inventory reservations and synchronizes them with purchase outcomes. */
@Service
@RequiredArgsConstructor
public class WarehouseServiceImp implements WarehouseService {
  private final InventoryRepository inventories;
  private final InventoryReservationRepository reservations;
  private final FlashSaleClient flashSaleClient;
  private final ObjectMapper objectMapper;
  private final AppProperties properties;

  /** Reserves inventory for a purchase, preserving idempotency by reservation key. */
  @Transactional
  public WarehouseDtos.Response reserve(WarehouseDtos.Request request) {
    UUID productId = request.getProductId();
    long quantity = request.getQuantity();
    UUID key = request.getReservationKey();

    if (quantity <= 0) {
      return responseFailure(INVALID, ServiceConstants.INVALID_QUANTITY_MESSAGE);
    }
    var existing = reservations.findByReservationKey(key);
    if (existing.isPresent()) {
      InventoryReservation reservation = existing.get();
      WarehouseDtos.Status status = WarehouseDtos.Status.fromString(reservation.getStatus());

      if (status.equals(RESERVED)) {
        status = ALREADY_RESERVED;
      } else if (status.equals(SOLD)) {
        status = ALREADY_SOLD;
      }
      boolean alreadyReserved =
          RESERVED.equals(WarehouseDtos.Status.fromString(reservation.getStatus()))
              || SOLD.equals(WarehouseDtos.Status.fromString(reservation.getStatus()));
      return alreadyReserved ? responseSuccess(status) : responseFailure(status);
    }

    int reservedInventoryRows = inventories.reserve(productId.toString(), quantity);
    if (reservedInventoryRows == 0) {
      return responseFailure(OUT_OF_STOCK);
    }
    reservations.save(new InventoryReservation(key, productId, quantity));
    return responseSuccess(RESERVED);
  }

  /** Releases an active inventory reservation. */
  @Transactional
  public WarehouseDtos.Response release(WarehouseDtos.Request request) {
    UUID key = request.getReservationKey();
    InventoryReservation reservation = reservations.findByReservationKey(key).orElse(null);
    if (ObjectUtils.isEmpty(reservation)) {
      return responseFailure(NOT_EXIST);
    }
    WarehouseDtos.Status orderStatus = WarehouseDtos.Status.fromString(reservation.getStatus());
    if (RELEASED.equals(orderStatus)) {
      return responseFailure(ALREADY_RELEASED);
    }
    if (SOLD.equals(orderStatus)) {
      return responseFailure(SOLD);
    }
    int releasedReservationRows = reservations.updateReservedStatus(key.toString(), RELEASED.name());
    if (releasedReservationRows == 0) {
      return responseFailure(ALREADY_RELEASED);
    }
    int releasedInventoryRows =
        inventories.release(reservation.getProductId().toString(), reservation.getQuantity());
    if (releasedInventoryRows == 0) {
      throw new IllegalStateException(ServiceConstants.INVENTORY_NOT_FOUND_WHILE_RELEASING);
    }
    return responseSuccess(RELEASED);
  }

  /** Marks a reservation as sold and decrements the corresponding inventory. */
  @Override
  @Transactional
  public WarehouseDtos.Response sold(WarehouseDtos.Request request) {
    UUID key = request.getReservationKey();
    InventoryReservation reservation = reservations.findByReservationKey(key).orElse(null);
    if (ObjectUtils.isEmpty(reservation)) {
      return responseFailure(NOT_EXIST);
    }
    WarehouseDtos.Status orderStatus = WarehouseDtos.Status.fromString(reservation.getStatus());
    if (SOLD.equals(orderStatus)) {
      return responseSuccess(SOLD);
    }
    if (!RESERVED.equals(orderStatus)) {
      return responseFailure(orderStatus);
    }
    int soldReservationRows = reservations.updateReservedStatus(key.toString(), SOLD.name());
    if (soldReservationRows == 0) {
      return responseFailure(ALREADY_SOLD);
    }
    int soldInventoryRows =
        inventories.sold(reservation.getProductId().toString(), reservation.getQuantity());
    if (soldInventoryRows == 0) {
      throw new IllegalStateException(ServiceConstants.INVENTORY_NOT_FOUND_WHILE_CONFIRMING);
    }
    return responseSuccess(SOLD);
  }

  /** Reconciles reserved inventory with purchase results from FlashSale. */
  @Override
  @Transactional
  public void syncPurchaseStatuses() {
    int batchSize = Math.max(1, properties.getStatusSync().getBatchSize());
    List<InventoryReservation> pending =
        reservations.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            RESERVED.name(),
            Instant.now().minus(properties.getStatusSync().getSyncAge()),
            PageRequest.of(0, batchSize));
    Map<UUID, InventoryReservation> byPurchaseId = new LinkedHashMap<>();
    for (InventoryReservation reservation : pending) {
      byPurchaseId.put(reservation.getReservationKey(), reservation);
    }
    if (byPurchaseId.isEmpty()) {
      return;
    }

    PurchaseStatusSyncDtos.Response response =
        requestStatuses(byPurchaseId.keySet().stream().toList());
    Optional.ofNullable(response.getPurchases())
        .orElseGet(Collections::emptyList)
        .forEach(
            entry -> {
              InventoryReservation reservation = byPurchaseId.get(entry.getPurchaseId());
              if (ObjectUtils.isNotEmpty(reservation)) {
                if (PurchaseStatus.SUCCESS.equals(entry.getStatus())) {
                  sold(
                      new WarehouseDtos.Request(null, reservation.getReservationKey(), null, null));
                } else if (PurchaseStatus.FAILED.equals(entry.getStatus())) {
                  release(
                      new WarehouseDtos.Request(null, reservation.getReservationKey(), null, null));
                }
              }
            });
  }

  /** Loads purchase statuses from FlashSale and maps transport failures to service errors. */
  private PurchaseStatusSyncDtos.Response requestStatuses(List<UUID> purchaseIds) {
    try {
      return objectMapper.readValue(
          flashSaleClient.getPurchasesStatus(purchaseIds), PurchaseStatusSyncDtos.Response.class);
    } catch (ApiException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException(
          "Cannot synchronize purchase statuses with FlashSale", exception);
    }
  }

  /** Creates a successful warehouse response with the status default message. */
  private WarehouseDtos.Response responseSuccess(WarehouseDtos.Status status) {
    return new WarehouseDtos.Response(true, status, status.defaultMessage, null, null);
  }

  /** Creates a failed warehouse response using the status default message. */
  private WarehouseDtos.Response responseFailure(WarehouseDtos.Status status) {
    return responseFailure(status, status.defaultMessage);
  }

  /** Creates a failed warehouse response with a custom message. */
  private WarehouseDtos.Response responseFailure(WarehouseDtos.Status status, String message) {
    return new WarehouseDtos.Response(false, status, message, null, null);
  }
}
