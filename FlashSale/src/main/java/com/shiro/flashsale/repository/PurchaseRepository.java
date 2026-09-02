package com.shiro.flashsale.repository;

import com.shiro.flashsale.constants.PurchaseStatus;
import com.shiro.flashsale.entity.Purchase;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
  int countByCustomerIdAndPurchaseDateAndStatusIsNot(
      UUID customerId, LocalDate purchaseDate, PurchaseStatus status);

  @Query(
      """
      select p from Purchase p join fetch p.item i join fetch i.product
      where p.customerId = :customerId order by p.createdAt desc
      """)
  List<Purchase> findHistory(@Param("customerId") UUID customerId, Pageable pageable);

  long countByItemIdAndPurchaseDate(UUID itemId, LocalDate purchaseDate);

  @EntityGraph(attributePaths = {"item", "item.product"})
  List<Purchase> findByStatusOrderByCreatedAtAsc(PurchaseStatus status, Pageable pageable);

  @Modifying
  @Query(
      value =
          "UPDATE flash_sale_purchases SET status = :status, updated_at = :updatedAt WHERE id = :purchaseId",
      nativeQuery = true)
  int updateStatus(
      @Param("purchaseId") String purchaseId,
      @Param("status") String status,
      @Param("updatedAt") java.time.Instant updatedAt);

  @Modifying
  @Query(
      value =
          "UPDATE flash_sale_purchases SET payment_transaction_id = :transactionId, updated_at = :updatedAt WHERE id = :purchaseId AND status = 'PENDING'",
      nativeQuery = true)
  int updatePaymentTransaction(
      @Param("purchaseId") String purchaseId,
      @Param("transactionId") String transactionId,
      @Param("updatedAt") java.time.Instant updatedAt);
}
