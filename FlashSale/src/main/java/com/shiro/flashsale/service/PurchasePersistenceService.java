package com.shiro.flashsale.service;

import com.shiro.flashsale.constants.PurchaseStatus;
import com.shiro.flashsale.entity.Purchase;
import com.shiro.flashsale.repository.PurchaseRepository;
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
  public void updateStatus(java.util.UUID purchaseId, PurchaseStatus status) {
    purchases.updateStatus(purchaseId, status);
  }
}
