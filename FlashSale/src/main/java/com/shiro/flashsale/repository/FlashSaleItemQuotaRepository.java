package com.shiro.flashsale.repository;

import com.shiro.flashsale.entity.FlashSaleItemQuota;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlashSaleItemQuotaRepository extends JpaRepository<FlashSaleItemQuota, UUID> {

  boolean existsByFlashSaleItemIdAndSaleDate(UUID flashSaleItemId, LocalDate saleDate);

  java.util.Optional<FlashSaleItemQuota> findByFlashSaleItemIdAndSaleDate(
      UUID flashSaleItemId, LocalDate saleDate);

  List<FlashSaleItemQuota> findByFlashSaleItemIdInAndSaleDate(
      Collection<UUID> flashSaleItemIds, LocalDate saleDate);

  /**
   * The single source of truth for "never oversell". The {@code remainingQuantity > 0} predicate is
   * evaluated by the database while it holds the row lock, so N concurrent callers produce at most
   * N successful decrements and never more than the configured quota.
   */
  @Modifying
  @Query(
      """
      update FlashSaleItemQuota q set q.remainingQuantity = q.remainingQuantity - :quota
      where q.flashSaleItemId = :itemId and q.saleDate = :saleDate and q.remainingQuantity > 0
      """)
  int decrement(
      @Param("itemId") UUID itemId,
      @Param("quota") int quota,
      @Param("saleDate") LocalDate saleDate);

  /** Compensating update used when a downstream step of the purchase fails outside the tx. */
  @Modifying
  @Query(
      """
      update FlashSaleItemQuota q set q.remainingQuantity = 0
      where q.flashSaleItemId in :itemIds and q.saleDate = :saleDate
      """)
  int closeQuotas(
      @Param("itemIds") Collection<UUID> itemIds, @Param("saleDate") LocalDate saleDate);
}
