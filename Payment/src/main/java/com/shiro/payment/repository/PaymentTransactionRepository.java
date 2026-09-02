package com.shiro.payment.repository;

import com.shiro.payment.domain.PaymentTransaction;
import com.shiro.payment.domain.TransactionType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
  Optional<PaymentTransaction> findByPurchaseIdAndType(UUID purchaseId, TransactionType type);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from PaymentTransaction t where t.purchaseId = :purchaseId and t.type = :type")
  Optional<PaymentTransaction> findForUpdate(
      @Param("purchaseId") UUID purchaseId, @Param("type") TransactionType type);

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
      UPDATE payment_transactions
      SET status = :newStatus, failure_reason = NULL, updated_at = :updatedAt
      WHERE id = :transactionId AND status = :expectedStatus
      """,
      nativeQuery = true)
  int updateStatus(
      @Param("transactionId") String transactionId,
      @Param("expectedStatus") String expectedStatus,
      @Param("newStatus") String newStatus,
      @Param("updatedAt") java.time.Instant updatedAt);
}
