package com.shiro.flashsale.service;

import com.shiro.flashsale.constants.PurchaseStatus;
import com.shiro.flashsale.constants.ServiceConstants;
import com.shiro.flashsale.entity.Purchase;
import com.shiro.flashsale.repository.PurchaseRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Encapsulates persistence operations that advance a purchase through its lifecycle. */
@Service
@RequiredArgsConstructor
public class PurchasePersistenceService {
  private final PurchaseRepository purchases;

  /** Persists a newly created purchase in the pending state. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Purchase createPending(Purchase purchase) {
    return purchases.saveAndFlush(purchase);
  }

  /** Updates a purchase status and flushes the change immediately. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateStatus(UUID purchaseId, PurchaseStatus status) {
    if (purchases.updateStatus(purchaseId.toString(), status.name(), Instant.now()) != 1)
      throw new IllegalStateException(ServiceConstants.PURCHASE_STATUS_UPDATE_FAILED + purchaseId);
  }

  /** Associates a payment transaction with a purchase awaiting payment completion. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void paymentPending(UUID purchaseId, UUID transactionId) {
    if (purchases.updatePaymentTransaction(
            purchaseId.toString(), transactionId.toString(), Instant.now())
        != 1)
      throw new IllegalStateException(ServiceConstants.PURCHASE_PAYMENT_UPDATE_FAILED + purchaseId);
  }
}
