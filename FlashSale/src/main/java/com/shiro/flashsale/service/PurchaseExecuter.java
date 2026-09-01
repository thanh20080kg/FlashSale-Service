package com.shiro.flashsale.service;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.constants.PurchaseStatus;
import com.shiro.flashsale.dto.SaleDtos;
import com.shiro.flashsale.entity.FlashSaleItem;
import com.shiro.flashsale.entity.Purchase;
import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import com.shiro.flashsale.repository.PurchaseRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PurchaseExecuter {
  private final PurchaseRepository purchases;
  private final FlashSaleItemQuotaRepository quotas;
  private final EntityManager entityManager;
  private final WarehouseService warehouse;
  private final PurchasePersistenceService purchasePersistence;
  private final AppProperties properties;

  @Transactional
  public SaleDtos.PurchaseResponse execute(UUID userId, FlashSaleItem item, LocalDate saleDate) {
    Instant now = Instant.now();
    FlashSaleItem itemRef = entityManager.getReference(FlashSaleItem.class, item.getId());

    Purchase purchase = new Purchase(userId, itemRef, item.getAmount(), saleDate, now);
    if (purchases.countByCustomerIdAndPurchaseDateAndStatusIsNot(
            userId, saleDate, PurchaseStatus.FAILED)
        > properties.getSale().getLimitDailyPurchase()) {
      throw ApiException.of(ErrorCode.DAILY_LIMIT_REACHED);
    }
    purchase = purchasePersistence.createPending(purchase);

    String reservationKey = purchase.getId().toString();
    try {
      warehouse.reserve(item.getProduct().getId(), reservationKey);
      if (quotas.decrement(item.getId(), 1, saleDate) == 0) {
        throw ApiException.of(ErrorCode.SOLD_OUT);
      }
      purchasePersistence.updateStatus(purchase.getId(), PurchaseStatus.SUCCESS);
    } catch (RuntimeException failure) {
      try {
        warehouse.release(reservationKey, item.getProduct().getId());
      } catch (RuntimeException releaseFailure) {
        failure.addSuppressed(releaseFailure);
      }
      purchasePersistence.updateStatus(purchase.getId(), PurchaseStatus.FAILED);
      throw failure;
    }

    try {
      warehouse.sold(item.getProduct().getId(), reservationKey);
    } catch (final RuntimeException failure) {
      // Warehouse reconciles this status when its Kafka-triggered sync runs.
    }

    return new SaleDtos.PurchaseResponse(
        purchase.getId(),
        item.getId(),
        item.getProduct().getSku(),
        item.getAmount(),
        saleDate,
        "SUCCESS",
        "Purchase successful");
  }
}
