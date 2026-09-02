package com.shiro.flashsale.service;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.constants.PurchaseStatus;
import com.shiro.flashsale.constants.ServiceConstants;
import com.shiro.flashsale.entity.Purchase;
import com.shiro.flashsale.repository.PurchaseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentStatusSyncService {
  private static final Logger log = LoggerFactory.getLogger(PaymentStatusSyncService.class);
  private final PurchaseRepository purchases;
  private final PurchasePersistenceService persistence;
  private final PaymentService payment;
  private final WarehouseService warehouse;
  private final com.shiro.flashsale.repository.FlashSaleItemQuotaRepository quotas;
  private final AppProperties properties;

  public void syncPending() {
    List<Purchase> batch =
        purchases.findByStatusOrderByCreatedAtAsc(
            PurchaseStatus.PENDING, PageRequest.of(0, properties.getPayment().getSyncBatchSize()));
    for (Purchase purchase : batch) sync(purchase);
  }

  private void sync(Purchase purchase) {
    try {
      var status = payment.status(purchase.getId());
      if (ServiceConstants.COMPLETE.equals(status.status())) {
        persistence.updateStatus(purchase.getId(), PurchaseStatus.SUCCESS);
        try {
          warehouse.sold(purchase.getItem().getProduct().getId(), purchase.getId().toString());
        } catch (RuntimeException exception) {
          log.warn("Could not finalize warehouse for {}", purchase.getId(), exception);
        }
      } else if (ServiceConstants.FAILED.equals(status.status())
          || ServiceConstants.CANCELLED.equals(status.status())) {
        persistence.updateStatus(purchase.getId(), PurchaseStatus.FAILED);
        restoreQuota(purchase);
        try {
          warehouse.release(purchase.getId().toString(), purchase.getItem().getProduct().getId());
        } catch (RuntimeException exception) {
          log.warn("Could not release warehouse for {}", purchase.getId(), exception);
        }
      }
    } catch (RuntimeException exception) {
      log.warn("Payment status sync failed for purchase {}", purchase.getId(), exception);
    }
  }

  void restoreQuota(Purchase purchase) {
    quotas.restore(purchase.getItem().getId().toString(), purchase.getPurchaseDate());
  }
}
