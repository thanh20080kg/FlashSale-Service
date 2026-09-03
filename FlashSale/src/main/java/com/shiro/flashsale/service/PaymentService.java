package com.shiro.flashsale.service;

import com.shiro.flashsale.client.PaymentClient;
import com.shiro.flashsale.constants.PurchaseStatus;
import com.shiro.flashsale.constants.ServiceConstants;
import com.shiro.flashsale.dto.client.PaymentDtos;
import com.shiro.flashsale.entity.Purchase;
import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import com.shiro.flashsale.repository.PurchaseRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class PaymentService {
  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
  private final PaymentClient client;
  private final ObjectMapper objectMapper;
  private final WarehouseService warehouse;
  private final PurchaseRepository purchases;
  private final FlashSaleItemQuotaRepository quotas;

  public PaymentDtos.Response pending(
      UUID purchaseId, UUID payerAccountId, UUID payeeAccountId, BigDecimal amount) {
    return send(
        PaymentDtos.Request.builder()
            .operation(PaymentDtos.Operation.PENDING)
            .purchaseId(purchaseId)
            .payerAccountId(payerAccountId)
            .payeeAccountId(payeeAccountId)
            .amount(amount)
            .build());
  }

  public PaymentDtos.Response confirm(UUID purchaseId) {
    return send(
        PaymentDtos.Request.builder()
            .operation(PaymentDtos.Operation.CONFIRM)
            .purchaseId(purchaseId)
            .build());
  }

  public PaymentDtos.Response cancel(UUID purchaseId) {
    return send(
        PaymentDtos.Request.builder()
            .operation(PaymentDtos.Operation.CANCEL)
            .purchaseId(purchaseId)
            .build());
  }

  public PaymentDtos.Response getStatus(UUID purchaseId) {
    return send(
        PaymentDtos.Request.builder()
            .operation(PaymentDtos.Operation.STATUS)
            .purchaseId(purchaseId)
            .build());
  }

  private PaymentDtos.Response send(PaymentDtos.Request request) {
    try {
      return objectMapper.readValue(client.send(request), PaymentDtos.Response.class);
    } catch (Exception exception) {
      throw new PaymentCommunicationException(
          ServiceConstants.PAYMENT_SERVICE_UNAVAILABLE, exception);
    }
  }

  /** Synchronizes one purchase and its warehouse reservation in a transaction. */
  @Transactional
  public void sync(Purchase purchase) {
    switch (getStatus(purchase.getId()).getStatus()) {
      case COMPLETE -> handleCompletedPayment(purchase);
      case FAILED, CANCELLED -> handleFailedPayment(purchase);
    }
  }

  private void handleCompletedPayment(Purchase purchase) {
    updateStatus(purchase, PurchaseStatus.SUCCESS);
    try {
      warehouse.sold(purchase.getItem().getProduct().getId(), purchase.getId());
    } catch (RuntimeException exception) {
      log.warn("Could not finalize warehouse for {}", purchase.getId(), exception);
    }
  }

  private void handleFailedPayment(Purchase purchase) {
    failPurchaseAndReleaseReservation(purchase);
  }

  private void failPurchaseAndReleaseReservation(Purchase purchase) {
    updateStatus(purchase, PurchaseStatus.FAILED);
    quotas.restore(purchase.getItem().getId().toString(), 1, purchase.getPurchaseDate());
    try {
      warehouse.release(purchase.getId(), purchase.getItem().getProduct().getId());
    } catch (RuntimeException exception) {
      log.warn("Could not release warehouse for {}", purchase.getId(), exception);
    }
  }

  private void updateStatus(Purchase purchase, PurchaseStatus status) {
    if (purchases.updateStatus(purchase.getId().toString(), status.name(), Instant.now()) != 1) {
      throw new IllegalStateException("Could not update purchase status for " + purchase.getId());
    }
  }

  public static class PaymentCommunicationException extends RuntimeException {
    public PaymentCommunicationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
