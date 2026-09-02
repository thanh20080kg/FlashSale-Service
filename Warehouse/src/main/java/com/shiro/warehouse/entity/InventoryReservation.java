package com.shiro.warehouse.entity;

import com.shiro.warehouse.constants.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {
  @Id private UUID id;

  @Getter
  @Setter
  @Column(name = "reservation_key", nullable = false, unique = true, length = 190)
  private UUID reservationKey;

  @Getter
  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Getter
  @Column(nullable = false, length = 20)
  private String status;

  @Getter
  @Column(nullable = false)
  private long quantity;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected InventoryReservation() {}

  public InventoryReservation(UUID key, UUID productId, long quantity) {
    this.id = UUID.randomUUID();
    this.reservationKey = key;
    this.productId = productId;
    this.quantity = quantity;
    this.status = OrderStatus.RESERVED.name();
    this.createdAt = Instant.now();
  }
}
