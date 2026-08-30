package com.shiro.flashsale.entity;

import com.shiro.flashsale.constants.InventoryEventStatus;
import com.shiro.flashsale.constants.InventoryEventType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Transactional outbox row. It is written in the same database transaction as the change that
 * produced it, so an event exists if and only if the change was committed.
 *
 * <p>{@code eventKey} is a natural, caller-supplied idempotency key (for example {@code
 * PURCHASE:<purchaseId>}) with a unique index behind it: replaying a producer can never enqueue the
 * same logical change twice.
 */
@Entity
@Table(
    name = "inventory_sync_events",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_inventory_event_key", columnNames = "event_key"),
    indexes =
        @Index(name = "idx_inventory_event_claim", columnList = "status,available_at,created_at"))
public class InventoryEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "event_key", nullable = false, length = 190)
  private String eventKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 40)
  private InventoryEventType eventType;

  @Column(name = "product_id", nullable = false, length = 36)
  private UUID productId;

  @Column(name = "quantity_delta", nullable = false)
  private long quantityDelta;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InventoryEventStatus status;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "available_at", nullable = false)
  private Instant availableAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(name = "last_error", length = 500)
  private String lastError;

  protected InventoryEvent() {}

  public InventoryEvent(
      String eventKey,
      InventoryEventType eventType,
      UUID productId,
      long quantityDelta,
      Instant now) {
    this.eventKey = eventKey;
    this.eventType = eventType;
    this.productId = productId;
    this.quantityDelta = quantityDelta;
    this.status = InventoryEventStatus.PENDING;
    this.attempts = 0;
    this.availableAt = now;
    this.createdAt = now;
  }

  private static String truncate(String value) {
    if (ObjectUtils.isEmpty(value)) {
      return null;
    }
    return value.length() <= 500 ? value : value.substring(0, 500);
  }

  public UUID getId() {
    return id;
  }

  public String getEventKey() {
    return eventKey;
  }

  public InventoryEventType getEventType() {
    return eventType;
  }

  public UUID getProductId() {
    return productId;
  }

  public long getQuantityDelta() {
    return quantityDelta;
  }

  public InventoryEventStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public void markProcessed(Instant now) {
    this.status = InventoryEventStatus.PROCESSED;
    this.processedAt = now;
    this.attempts++;
    this.lastError = null;
  }

  public void markRetry(Instant nextAttemptAt, String error) {
    this.attempts++;
    this.availableAt = nextAttemptAt;
    this.lastError = truncate(error);
  }

  public void markFailed(Instant now, String error) {
    this.attempts++;
    this.status = InventoryEventStatus.FAILED;
    this.processedAt = now;
    this.lastError = truncate(error);
  }
}
