package com.shiro.payment.entity;

import com.shiro.payment.constants.TransactionStatus;
import com.shiro.payment.constants.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
    name = "payment_transactions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_payment_purchase_type",
            columnNames = {"purchase_id", "type"}))
public class PaymentTransaction {
  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "purchase_id", nullable = false)
  private UUID purchaseId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payer_account_id")
  private Account payerAccount;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payee_account_id")
  private Account payeeAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_transaction_id")
  private PaymentTransaction parentTransaction;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TransactionType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TransactionStatus status;

  @Column(name = "failure_reason")
  private String failureReason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PaymentTransaction() {}

  public PaymentTransaction(
      UUID purchaseId,
      Account payerAccount,
      Account payeeAccount,
      BigDecimal amount,
      TransactionType type,
      PaymentTransaction parent,
      TransactionStatus status,
      Instant now) {
    this.id = UUID.randomUUID();
    this.purchaseId = purchaseId;
    this.payerAccount = payerAccount;
    this.payeeAccount = payeeAccount;
    this.amount = amount;
    this.type = type;
    this.parentTransaction = parent;
    this.status = status;
    this.createdAt = now;
    this.updatedAt = now;
  }
}
