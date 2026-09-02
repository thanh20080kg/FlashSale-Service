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

@Service
@RequiredArgsConstructor
public class PurchasePersistenceService {
  private final PurchaseRepository purchases;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Purchase createPending(Purchase purchase) {
    return purchases.saveAndFlush(purchase);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateStatus(UUID purchaseId, PurchaseStatus status) {
    if (purchases.updateStatus(purchaseId.toString(), status.name(), Instant.now()) != 1)
      throw new IllegalStateException(ServiceConstants.PURCHASE_STATUS_UPDATE_FAILED + purchaseId);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void paymentPending(UUID purchaseId, UUID transactionId) {
    if (purchases.updatePaymentTransaction(
            purchaseId.toString(), transactionId.toString(), Instant.now())
        != 1)
      throw new IllegalStateException(ServiceConstants.PURCHASE_PAYMENT_UPDATE_FAILED + purchaseId);
  }
}
