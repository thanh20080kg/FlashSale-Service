package com.shiro.flashsale.repository;

import com.shiro.flashsale.entity.Customer;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
  Optional<Customer> findByUserId(UUID userId);

  /** Conditional debit: returns 0 instead of overdrawing when the balance is short. */
  @Modifying
  @Query(
      """
      update Customer c set c.balance = c.balance - :amount
      where c.id = :id and c.balance >= :amount
      """)
  int debit(@Param("id") UUID id, @Param("amount") BigDecimal amount);

  @Modifying
  @Query("update Customer c set c.balance = c.balance + :amount where c.id = :id")
  int credit(@Param("id") UUID id, @Param("amount") BigDecimal amount);
}
