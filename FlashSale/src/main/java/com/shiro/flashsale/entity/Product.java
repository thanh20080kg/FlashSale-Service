package com.shiro.flashsale.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
    name = "products",
    uniqueConstraints = @UniqueConstraint(name = "uk_product_sku", columnNames = "sku"))
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 64)
  private String sku;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false)
  private boolean active = true;

  protected Product() {}

  public Product(String sku, String name) {
    this.sku = sku;
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
