package com.shiro.flashsale.repository;

import com.shiro.flashsale.constants.PurchaseStatus;
import com.shiro.flashsale.entity.Purchase;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
  int countByCustomerIdAndPurchaseDateAndStatusIsNot(UUID customerId, LocalDate purchaseDate, PurchaseStatus status);

  @Query(
      """
      select p from Purchase p join fetch p.item i join fetch i.product
      where p.customerId = :customerId order by p.createdAt desc
      """)
  List<Purchase> findHistory(@Param("customerId") UUID customerId, Pageable pageable);

  long countByItemIdAndPurchaseDate(UUID itemId, LocalDate purchaseDate);

  @Modifying
  @Query("UPDATE Purchase p SET p.status = :status WHERE p.id = :purchaseId")
  int updateStatus(@Param("purchaseId") UUID purchaseId, @Param("status") PurchaseStatus status);
}
