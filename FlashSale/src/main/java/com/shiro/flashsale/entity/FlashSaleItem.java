package com.shiro.flashsale.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Configuration of one product inside one slot: price and the daily allowance.
 *
 * <p>The live counter is deliberately <em>not</em> here - it lives in {@link FlashSaleItemQuota},
 * keyed by day, because slots repeat daily.
 */
@Entity
@Table(
    name = "flash_sale_items",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_slot_product",
            columnNames = {"slot_id", "product_id"}))
public class FlashSaleItem {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "slot_id", nullable = false)
  private FlashSaleSlot slot;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  /** Units released per sale day. */
  @Column(nullable = false)
  private long quantity;

  @Column(nullable = false)
  private boolean active = true;

  protected FlashSaleItem() {}

  public FlashSaleItem(FlashSaleSlot slot, Product product, BigDecimal amount, long quantity) {
    this.slot = slot;
    this.product = product;
    this.amount = amount;
    this.quantity = quantity;
  }

  public UUID getId() {
    return id;
  }

  public FlashSaleSlot getSlot() {
    return slot;
  }

  public Product getProduct() {
    return product;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public long getQuantity() {
    return quantity;
  }

  public void setQuantity(long quantity) {
    this.quantity = quantity;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
