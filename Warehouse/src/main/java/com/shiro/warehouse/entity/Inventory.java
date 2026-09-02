package com.shiro.warehouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "inventories")
public class Inventory {
  @Id @Getter UUID id;

  @Column(name = "product_id", nullable = false, unique = true)
  private UUID productId;

  @Column(name = "available_quantity", nullable = false)
  private long availableQuantity;

  @Column(name = "sold_quantity", nullable = false)
  private long soldQuantity;

  protected Inventory() {}

  public Inventory(UUID productId, long quantity) {
    this.id = UUID.randomUUID();
    this.productId = productId;
    this.availableQuantity = quantity;
  }
}
