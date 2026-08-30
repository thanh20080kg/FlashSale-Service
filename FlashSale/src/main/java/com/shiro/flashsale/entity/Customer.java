package com.shiro.flashsale.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Sale-side projection of the customer row owned by Authentication. */
@Entity
@Table(name = "customers")
public class Customer {
  @Id
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "display_name", length = 100)
  private String displayName;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Customer() {}

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
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
