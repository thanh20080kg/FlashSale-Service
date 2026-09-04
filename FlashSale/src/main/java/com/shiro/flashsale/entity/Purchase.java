package com.shiro.flashsale.entity;

import com.shiro.flashsale.constants.PurchaseStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(
    name = "flash_sale_purchases",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_customer_purchase_day",
            columnNames = {"customer_id", "purchase_date"}))
public class Purchase {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "flash_sale_item_id", nullable = false)
  private FlashSaleItem item;

  @Column(nullable = false)
  private Integer quantity;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "purchase_date", nullable = false)
  private LocalDate purchaseDate;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "payment_transaction_id")
  private UUID paymentTransactionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PurchaseStatus status;

  protected Purchase() {}

  public Purchase(
      UUID customerId,
      FlashSaleItem item,
      Integer quantity,
      BigDecimal amount,
      LocalDate date,
      Instant createdAt) {
    this.customerId = customerId;
    this.item = item;
    this.quantity = quantity;
    this.amount = amount;
    this.purchaseDate = date;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
    this.status = PurchaseStatus.PENDING;
  }
}
