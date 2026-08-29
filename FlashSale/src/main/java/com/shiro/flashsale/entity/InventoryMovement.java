package com.shiro.flashsale.entity;

import com.shiro.flashsale.constants.InventoryEventType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only ledger of applied stock changes: the synchronised projection of the outbox. The
 * unique index on {@code event_id} is the second idempotency guard, so even a worker that somehow
 * replays a claimed event cannot double-apply it.
 */
@Entity
@Table(
    name = "inventory_movements",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_inventory_movement_event", columnNames = "event_id"),
    indexes = @Index(name = "idx_inventory_movement_product", columnList = "product_id,created_at"))
public class InventoryMovement {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "event_id", nullable = false, length = 36)
  private UUID eventId;

  @Column(name = "product_id", nullable = false, length = 36)
  private UUID productId;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 40)
  private InventoryEventType eventType;

  @Column(name = "quantity_delta", nullable = false)
  private long quantityDelta;

  @Column(name = "balance_after", nullable = false)
  private long balanceAfter;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected InventoryMovement() {}

  public InventoryMovement(
      UUID eventId,
      UUID productId,
      InventoryEventType eventType,
      long quantityDelta,
      long balanceAfter,
      Instant createdAt) {
    this.eventId = eventId;
    this.productId = productId;
    this.eventType = eventType;
    this.quantityDelta = quantityDelta;
    this.balanceAfter = balanceAfter;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getProductId() {
    return productId;
  }

  public long getQuantityDelta() {
    return quantityDelta;
  }

  public long getBalanceAfter() {
    return balanceAfter;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public InventoryEventType getEventType() {
    return eventType;
  }
}
