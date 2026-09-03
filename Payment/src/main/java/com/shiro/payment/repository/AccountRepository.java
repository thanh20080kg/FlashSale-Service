package com.shiro.payment.repository;

import com.shiro.payment.entity.Account;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {
  @Query(
      value = "SELECT COUNT(*) FROM accounts WHERE id = :accountId AND active = TRUE",
      nativeQuery = true)
  long countActive(@Param("accountId") String accountId);

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
      UPDATE accounts
      SET available_amount = available_amount - :amount,
          updated_at = :updatedAt
      WHERE id = :accountId
        AND active = TRUE
        AND available_amount >= :amount
      """,
      nativeQuery = true)
  int hold(
      @Param("accountId") String accountId,
      @Param("amount") BigDecimal amount,
      @Param("updatedAt") Instant updatedAt);

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
      UPDATE accounts
      SET current_amount = current_amount - :amount,
          updated_at = :updatedAt
      WHERE id = :accountId
        AND current_amount >= :amount
        AND current_amount - :amount >= available_amount
      """,
      nativeQuery = true)
  int capture(
      @Param("accountId") String accountId,
      @Param("amount") BigDecimal amount,
      @Param("updatedAt") Instant updatedAt);

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
      UPDATE accounts
      SET current_amount = current_amount + :amount,
          available_amount = available_amount + :amount,
          updated_at = :updatedAt
      WHERE id = :accountId
      """,
      nativeQuery = true)
  int credit(
      @Param("accountId") String accountId,
      @Param("amount") BigDecimal amount,
      @Param("updatedAt") Instant updatedAt);

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
      UPDATE accounts
      SET available_amount = available_amount + :amount,
          updated_at = :updatedAt
      WHERE id = :accountId
        AND available_amount + :amount <= current_amount
      """,
      nativeQuery = true)
  int release(
      @Param("accountId") String accountId,
      @Param("amount") BigDecimal amount,
      @Param("updatedAt") Instant updatedAt);
}
