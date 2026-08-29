package com.shiro.flashsale.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * On-hand stock of a product.
 *
 * <p>{@code availableQuantity} is the authoritative live counter and is only ever moved by
 * conditional atomic UPDATEs inside the purchase transaction, so it can never go negative. {@code
 * soldQuantity} is the asynchronously synchronised projection maintained by the inventory sync
 * worker from the outbox.
 */
@Entity
@Table(
    name = "inventories",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_inventory_product", columnNames = "product_id"))
public class Inventory {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(nullable = false)
  private long availableQuantity;

  @Column(nullable = false)
  private long soldQuantity;

  protected Inventory() {}

  public Inventory(Product product, long availableQuantity) {
    this.product = product;
    this.availableQuantity = availableQuantity;
  }

  public UUID getId() {
    return id;
  }

  public Product getProduct() {
    return product;
  }

  public long getAvailableQuantity() {
    return availableQuantity;
  }

  public long getSoldQuantity() {
    return soldQuantity;
  }
}
