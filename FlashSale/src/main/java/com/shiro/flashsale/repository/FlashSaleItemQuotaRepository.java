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
import org.springframework.transaction.annotation.Transactional;

public interface FlashSaleItemQuotaRepository extends JpaRepository<FlashSaleItemQuota, UUID> {

  boolean existsByFlashSaleItemIdAndSaleDate(UUID flashSaleItemId, LocalDate saleDate);

  java.util.Optional<FlashSaleItemQuota> findByFlashSaleItemIdAndSaleDate(
      UUID flashSaleItemId, LocalDate saleDate);

  List<FlashSaleItemQuota> findByFlashSaleItemIdInAndSaleDate(
      Collection<UUID> flashSaleItemIds, LocalDate saleDate);

  @Modifying
  @Query(
      value =
          "UPDATE flash_sale_item_quotas SET remaining_quantity = remaining_quantity - :quota WHERE flash_sale_item_id = :itemId AND sale_date = :saleDate AND remaining_quantity >= :quota",
      nativeQuery = true)
  int decrement(
      @Param("itemId") String itemId,
      @Param("quota") int quota,
      @Param("saleDate") LocalDate saleDate);

  @Modifying
  @Transactional
  @Query(
      value =
      "UPDATE flash_sale_item_quotas SET remaining_quantity = remaining_quantity + :quantity WHERE flash_sale_item_id = :itemId AND sale_date = :saleDate AND remaining_quantity + :quantity <= total_quantity",
      nativeQuery = true)
  int restore(
      @Param("itemId") String itemId,
      @Param("quantity") int quantity,
      @Param("saleDate") LocalDate saleDate);

  @Modifying
  @Query(
      value =
          "UPDATE flash_sale_item_quotas SET remaining_quantity = 0 WHERE flash_sale_item_id IN (:itemIds) AND sale_date = :saleDate",
      nativeQuery = true)
  int closeQuotas(
      @Param("itemIds") Collection<String> itemIds, @Param("saleDate") LocalDate saleDate);
}
