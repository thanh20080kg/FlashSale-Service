package com.shiro.authentication.repository;

import com.shiro.authentication.entity.Customer;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
  Optional<Customer> findByUserId(UUID userId);

  @Modifying
  @Query(
      value =
          "UPDATE customers SET balance = balance - :amount WHERE id = :id AND balance >= :amount",
      nativeQuery = true)
  int debit(@Param("id") String id, @Param("amount") BigDecimal amount);

  @Modifying
  @Query(
      value = "UPDATE customers SET balance = balance + :amount WHERE id = :id",
      nativeQuery = true)
  int credit(@Param("id") String id, @Param("amount") BigDecimal amount);
}
