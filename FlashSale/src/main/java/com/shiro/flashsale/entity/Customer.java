package com.shiro.flashsale.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Customer profile belonging to an authenticated {@link User}.
 *
 * <p>{@code balance} is only ever moved by the conditional atomic UPDATEs in {@code
 * CustomerRepository}, never by dirty-checking, so concurrent purchases cannot overdraw it.
 */
@Entity
@Table(
    name = "customers",
    uniqueConstraints = @UniqueConstraint(name = "uk_customer_user", columnNames = "user_id"))
public class Customer {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(length = 100)
  private String displayName;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Customer() {}

  public Customer(User user, String displayName, Instant createdAt) {
    this.user = user;
    this.displayName = displayName;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public String getDisplayName() {
    return displayName;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
