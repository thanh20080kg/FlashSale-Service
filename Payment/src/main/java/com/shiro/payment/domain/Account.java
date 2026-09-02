package com.shiro.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "accounts")
public class Account {
  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID id;

  @Column(name = "current_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal currentAmount;

  @Column(name = "available_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal availableAmount;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Account() {}
}
