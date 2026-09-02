package com.shiro.flashsale.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

  @Column(name = "owner_id")
  private UUID ownerId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "slot_id", nullable = false)
  private FlashSaleSlot slot;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

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
}
