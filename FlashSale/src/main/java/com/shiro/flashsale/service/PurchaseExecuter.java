package com.shiro.flashsale.service;

import com.shiro.flashsale.constants.PurchaseStatus;
import com.shiro.flashsale.constants.ServiceConstants;
import com.shiro.flashsale.dto.PurchaseDtos;
import com.shiro.flashsale.dto.client.PaymentDtos;
import com.shiro.flashsale.entity.FlashSaleItem;
import com.shiro.flashsale.entity.Purchase;
import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import com.shiro.flashsale.exception.PurchaseResponseException;
import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import com.shiro.flashsale.repository.PurchaseRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Orchestrates the reservation, payment, and completion steps of a purchase. */
@Service
@AllArgsConstructor
public class PurchaseExecuter {
  private static final Logger log = LoggerFactory.getLogger(PurchaseExecuter.class);
  private final PurchaseRepository purchases;
  private final FlashSaleItemQuotaRepository quotas;
  private final EntityManager entityManager;
  private final WarehouseService warehouse;
  private final PaymentService payment;
  private final PurchasePersistenceService purchasePersistence;
  private final CacheConfigService cacheConfigService;

  @Transactional
  public PurchaseDtos.PurchaseResponse execute(
      UUID userId, FlashSaleItem item, Integer quantity, LocalDate saleDate) {
    Instant now = Instant.now();
    FlashSaleItem itemRef = entityManager.getReference(FlashSaleItem.class, item.getId());
    Purchase purchase = new Purchase(userId, itemRef, quantity, item.getAmount(), saleDate, now);

    preValidate(purchase, saleDate);
    purchase = purchasePersistence.createPending(purchase);

    reservingProcess(purchase);
    return confirmingProcess(purchase);
  }

  /** Synchronizes one purchase and its warehouse reservation in a transaction. */
  @Transactional
  public void sync(Purchase purchase) {
    switch (payment.getStatus(purchase.getId()).getStatus()) {
      case COMPLETE -> commitReservation(purchase);
      case PENDING -> confirmingProcess(purchase);
      case FAILED, CANCELLED -> handleFailedPayment(purchase);
    }
  }

  private void preValidate(Purchase purchase, LocalDate saleDate) {
    long dailyPurchaseCount =
        purchases.countByCustomerIdAndPurchaseDateAndStatusIsNot(
            purchase.getCustomerId(), saleDate, PurchaseStatus.FAILED);
    int dailyPurchaseLimit = cacheConfigService.getLimitDailyPurchase();
    if (dailyPurchaseCount >= dailyPurchaseLimit) {
      log.warn(
          "Purchase rejected: daily limit reached, userId={}, itemId={}",
          purchase.getCustomerId(),
          purchase.getItem().getId());
      throw ApiException.of(ErrorCode.DAILY_LIMIT_REACHED);
    }
    // Todo check if user has enough balance
  }

  private void reservingProcess(Purchase purchase) {
    checkAndConsumeQuota(purchase);
    PaymentDtos.Response pending = createPendingPayment(purchase);
    updateStatusAndReserveWarehouse(purchase, pending);
  }

  private PurchaseDtos.PurchaseResponse confirmingProcess(Purchase purchase) {
    try {
      // Confirm payment
      PaymentDtos.Response confirmed = payment.confirm(purchase.getId());
      if (!confirmed.isSuccess()
          || !PaymentDtos.PaymentStatus.COMPLETE.equals(confirmed.getStatus())) {

        // Mark failed if payment failed, throw error to rollback
        purchasePersistence.updateStatus(purchase.getId(), PurchaseStatus.FAILED);
        warehouse.release(purchase.getId(), purchase.getItem().getProduct().getId());
        throw new PurchaseResponseException(failedResponse(purchase));
      }
      // Confirm purchase to warehouse and update status
      commitReservation(purchase);
    } catch (PurchaseResponseException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "Failed to confirm purchase, purchaseId={}, itemId={}",
          purchase.getId(),
          purchase.getItem().getId(),
          e);
      // Mark pending if payment failed, waiting job check status
      return pendingResponse(purchase);
    }
    return successResponse(purchase);
  }

  /** Uses Redis as a fast quota guard and the database as the source of truth. */
  private void checkAndConsumeQuota(Purchase purchase) {
    FlashSaleItem item = purchase.getItem();
    var consumedQuotas =
        quotas.decrement(
            item.getId().toString(), purchase.getQuantity(), purchase.getPurchaseDate());
    if (consumedQuotas == 0) {
      log.warn(
          "Purchase rejected: item sold out, purchaseId={}, itemId={}",
          purchase.getId(),
          item.getId());
      throw ApiException.of(ErrorCode.SOLD_OUT);
    }
  }

  /** Requests a payment hold for the purchase. */
  private PaymentDtos.Response createPendingPayment(Purchase purchase) {
    try {
      PaymentDtos.Response pending =
          payment.pending(
              purchase.getId(),
              purchase.getCustomerId(),
              purchase.getItem().getProduct().getOwnerId(),
              purchase.getAmount());
      if (!pending.isSuccess() || !PaymentDtos.PaymentStatus.PENDING.equals(pending.getStatus())) {
        purchasePersistence.updateStatus(purchase.getId(), PurchaseStatus.FAILED);
        log.warn("Purchase rejected during payment pending, purchaseId={}", purchase.getId());
        throw ApiException.of(ErrorCode.PAYMENT_FAILED);
      }
      return pending;
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to send payment pending request, purchaseId={}", purchase.getId(), e);
      throw ApiException.of(ErrorCode.PAYMENT_FAILED);
    }
  }

  /** Persists the payment transaction and reserves the product in Warehouse. */
  private void updateStatusAndReserveWarehouse(Purchase purchase, PaymentDtos.Response pending) {
    FlashSaleItem item = purchase.getItem();
    try {
      purchasePersistence.paymentPending(purchase.getId(), pending.getTransactionId());
      warehouse.reserve(item.getProduct().getId(), purchase.getId(), purchase.getQuantity());
    } catch (Exception failure) {
      log.error(
          "Reservation process failed, purchaseId={}, itemId={}",
          purchase.getId(),
          item.getId(),
          failure);
      releaseReservation(purchase);
      throw failure;
    }
  }

  private void releaseReservation(Purchase purchase) {
    try {
      purchasePersistence.updateStatus(purchase.getId(), PurchaseStatus.FAILED);
      payment.cancel(purchase.getId());
      warehouse.release(purchase.getId(), purchase.getItem().getProduct().getId());
    } catch (RuntimeException e) {
      log.error("Failed when release reservation, purchaseId={}", purchase.getId(), e);
    }
  }

  private void commitReservation(Purchase purchase) {
    try {
      purchasePersistence.updateStatus(purchase.getId(), PurchaseStatus.SUCCESS);
      warehouse.sold(purchase.getItem().getProduct().getId(), purchase.getId());
    } catch (RuntimeException e) {
      log.error("Failed when confirm reservation, purchaseId={}", purchase.getId(), e);
    }
  }

  private PurchaseDtos.PurchaseResponse successResponse(Purchase purchase) {
    return purchaseResponse(
        purchase, PurchaseStatus.SUCCESS, ServiceConstants.PURCHASE_SUCCESS_MESSAGE);
  }

  private PurchaseDtos.PurchaseResponse failedResponse(Purchase purchase) {
    return purchaseResponse(purchase, PurchaseStatus.FAILED, ServiceConstants.PAYMENT_FAILED);
  }

  private PurchaseDtos.PurchaseResponse pendingResponse(Purchase purchase) {
    return purchaseResponse(
        purchase, PurchaseStatus.PENDING, ServiceConstants.PAYMENT_RETRY_MESSAGE);
  }

  private PurchaseDtos.PurchaseResponse purchaseResponse(
      Purchase purchase, PurchaseStatus status, String message) {
    FlashSaleItem item = purchase.getItem();
    return PurchaseDtos.PurchaseResponse.builder()
        .purchaseId(purchase.getId())
        .itemId(item.getId())
        .sku(item.getProduct().getSku())
        .amount(purchase.getAmount())
        .purchaseDate(purchase.getPurchaseDate())
        .status(status.name())
        .message(message)
        .build();
  }

  private void handleFailedPayment(Purchase purchase) {
    int updatedRows =
        purchases.updateStatus(
            purchase.getId().toString(), PurchaseStatus.FAILED.name(), Instant.now());
    if (updatedRows != 1) {
      throw new IllegalStateException("Could not update purchase status for " + purchase.getId());
    }
    quotas.restore(
        purchase.getItem().getId().toString(), purchase.getQuantity(), purchase.getPurchaseDate());

    try {
      warehouse.release(purchase.getId(), purchase.getItem().getProduct().getId());
    } catch (RuntimeException exception) {
      log.warn("Could not release warehouse for {}", purchase.getId(), exception);
    }
  }
}
