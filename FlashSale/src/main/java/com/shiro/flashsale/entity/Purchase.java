package com.shiro.flashsale.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One successful flash sale purchase.
 *
 * <p>The unique key on {@code (customer_id, purchase_date)} is what actually enforces "one purchase
 * per user per day" - the pre-check in the service is only a fast path, the index is the guarantee
 * that survives concurrent requests hitting different instances.
 */
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

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "flash_sale_item_id", nullable = false)
  private FlashSaleItem item;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "purchase_date", nullable = false)
  private LocalDate purchaseDate;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Purchase() {}

  public Purchase(
      Customer customer, FlashSaleItem item, BigDecimal amount, LocalDate date, Instant createdAt) {
    this.customer = customer;
    this.item = item;
    this.amount = amount;
    this.purchaseDate = date;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public Customer getCustomer() {
    return customer;
  }

  public FlashSaleItem getItem() {
    return item;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDate getPurchaseDate() {
    return purchaseDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
