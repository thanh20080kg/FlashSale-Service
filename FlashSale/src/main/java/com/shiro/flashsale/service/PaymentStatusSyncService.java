package com.shiro.flashsale.service;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.constants.PurchaseStatus;
import com.shiro.flashsale.entity.Purchase;
import com.shiro.flashsale.repository.PurchaseRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Reconciles pending purchases with payment results and warehouse reservations. */
@Service
@RequiredArgsConstructor
public class PaymentStatusSyncService {
  private static final Logger log = LoggerFactory.getLogger(PaymentStatusSyncService.class);
  private final AppProperties properties;
  private final PurchaseRepository purchases;
  private final PaymentService payment;

  /** Processes only pending purchases older than five minutes. */
  public void syncPending() {
    List<Purchase> batch =
        purchases.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            PurchaseStatus.PENDING,
            Instant.now().minus(properties.getPayment().getSyncAge()),
            PageRequest.of(0, properties.getPayment().getSyncBatchSize()));
    for (Purchase purchase : batch) {
      try {
        payment.sync(purchase);
      } catch (Exception exception) {
        log.error(
            "Failed to sync purchase status for purchase id: {}", purchase.getId(), exception);
      }
    }
  }
}
