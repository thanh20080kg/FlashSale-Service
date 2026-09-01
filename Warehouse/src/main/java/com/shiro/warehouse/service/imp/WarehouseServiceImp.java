package com.shiro.warehouse.service.imp;

import static com.shiro.warehouse.constants.OrderStatus.*;

import com.shiro.warehouse.client.FlashSaleClient;
import com.shiro.warehouse.constants.OrderStatus;
import com.shiro.warehouse.constants.PurchaseStatus;
import com.shiro.warehouse.dto.PurchaseStatusSyncDtos;
import com.shiro.warehouse.dto.WarehouseRequest;
import com.shiro.warehouse.dto.WarehouseResponse;
import com.shiro.warehouse.entity.Inventory;
import com.shiro.warehouse.entity.InventoryReservation;
import com.shiro.warehouse.repository.InventoryRepository;
import com.shiro.warehouse.repository.InventoryReservationRepository;
import com.shiro.warehouse.service.WarehouseService;
import jakarta.transaction.Transactional;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.kafka.common.errors.ApiException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImp implements WarehouseService {
  private final InventoryRepository inventories;
  private final InventoryReservationRepository reservations;
  private final FlashSaleClient flashSaleClient;
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.status-sync.rabbit-queue:flashsale.purchase-status.sync}")
  private String statusSyncQueue;

  @Transactional
  public WarehouseResponse reserve(WarehouseRequest command) {
    UUID productId = command.productId();
    long quantity = command.quantity();
    UUID key = command.reservationKey();

    if (quantity <= 0) {
      return new WarehouseResponse(false, INVALID, "quantity must be greater than zero");
    }
    var existing = reservations.findByReservationKey(key);
    if (existing.isPresent()) {
      InventoryReservation reservation = existing.get();
      OrderStatus orderStatus = OrderStatus.fromString(reservation.getStatus());

      if (orderStatus.equals(RESERVED)) {
        orderStatus = ALREADY_RESERVED;
      } else if (orderStatus.equals(SOLD)) {
        orderStatus = ALREADY_SOLD;
      }
      return new WarehouseResponse(
          RESERVED.equals(OrderStatus.fromString(reservation.getStatus()))
              || SOLD.equals(OrderStatus.fromString(reservation.getStatus())),
          orderStatus,
          orderStatus.defaultMessage);
    }

    Inventory inventory = inventories.findByProductId(productId).orElse(null);
    if (inventory == null || inventory.getAvailableQuantity() < quantity) {
      return new WarehouseResponse(false, OUT_OF_STOCK, OUT_OF_STOCK.defaultMessage);
    }

    if (inventories.reserve(productId, quantity) == 0) {
      return new WarehouseResponse(false, OUT_OF_STOCK, OUT_OF_STOCK.defaultMessage);
    }
    reservations.save(new InventoryReservation(key, productId, quantity));
    return new WarehouseResponse(true, RESERVED, RESERVED.defaultMessage);
  }

  @Transactional
  public WarehouseResponse release(WarehouseRequest command) {
    UUID key = command.reservationKey();
    InventoryReservation reservation = reservations.findByReservationKey(key).orElse(null);
    if (ObjectUtils.isEmpty(reservation)) {
      return new WarehouseResponse(false, NOT_EXIST, NOT_EXIST.defaultMessage);
    }
    OrderStatus orderStatus = OrderStatus.fromString(reservation.getStatus());
    if (RELEASED.equals(orderStatus)) {
      return new WarehouseResponse(false, ALREADY_RELEASED, ALREADY_RELEASED.defaultMessage);
    }
    if (SOLD.equals(orderStatus)) {
      return new WarehouseResponse(false, SOLD, SOLD.defaultMessage);
    }
    if (reservations.updateReservedStatus(key, RELEASED.name()) == 0) {
      return new WarehouseResponse(false, ALREADY_RELEASED, ALREADY_RELEASED.defaultMessage);
    }
    if (inventories.release(reservation.getProductId(), reservation.getQuantity()) == 0) {
      throw new IllegalStateException("Inventory was not found while releasing reservation");
    }
    return new WarehouseResponse(true, RELEASED, RELEASED.defaultMessage);
  }

  @Override
  @Transactional
  public WarehouseResponse sold(WarehouseRequest command) {
    UUID key = command.reservationKey();
    InventoryReservation reservation = reservations.findByReservationKey(key).orElse(null);
    if (ObjectUtils.isEmpty(reservation)) {
      return new WarehouseResponse(false, NOT_EXIST, NOT_EXIST.defaultMessage);
    }
    OrderStatus orderStatus = OrderStatus.fromString(reservation.getStatus());
    if (SOLD.equals(orderStatus)) {
      return new WarehouseResponse(true, SOLD, SOLD.defaultMessage);
    }
    if (!RESERVED.equals(orderStatus)) {
      return new WarehouseResponse(false, orderStatus, orderStatus.defaultMessage);
    }
    if (reservations.updateReservedStatus(key, SOLD.name()) == 0) {
      return new WarehouseResponse(false, ALREADY_SOLD, ALREADY_SOLD.defaultMessage);
    }
    if (inventories.sold(reservation.getProductId(), reservation.getQuantity()) == 0) {
      throw new IllegalStateException("Inventory was not found while confirming reservation");
    }
    return new WarehouseResponse(true, SOLD, SOLD.defaultMessage);
  }

  @Override
  @Transactional
  public void syncPurchaseStatuses() {
    List<InventoryReservation> pending = reservations.findByStatus(RESERVED.name());
    Map<UUID, InventoryReservation> byPurchaseId = new LinkedHashMap<>();
    for (InventoryReservation reservation : pending) {
      byPurchaseId.put(reservation.getReservationKey(), reservation);
    }
    if (byPurchaseId.isEmpty()) {
      return;
    }

    PurchaseStatusSyncDtos.Response response =
        requestStatuses(byPurchaseId.keySet().stream().toList());

    // Update purchase statuses
    Optional.ofNullable(response.purchases())
        .orElseGet(Collections::emptyList)
        .forEach(
            entry -> {
              InventoryReservation reservation = byPurchaseId.get(entry.purchaseId());
              if (ObjectUtils.isNotEmpty(reservation)) {
                if (PurchaseStatus.SUCCESS.equals(entry.status())) {
                  sold(new WarehouseRequest(null, reservation.getReservationKey(), null, 0));
                } else if (PurchaseStatus.FAILED.equals(entry.status())) {
                  release(new WarehouseRequest(null, reservation.getReservationKey(), null, 0));
                }
              }
            });
  }

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
}
