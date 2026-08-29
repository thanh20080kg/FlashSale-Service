package com.shiro.flashsale.service;

import com.shiro.flashsale.constants.InventoryEventType;
import java.util.UUID;

public interface InventorySyncService {

  /**
   * Enqueues an inventory change on the transactional outbox.
   *
   * <p>Must be called inside the transaction that performs the change itself. {@code eventKey} is
   * the idempotency key: enqueueing the same key twice is a no-op, not a duplicate.
   */
  void enqueue(String eventKey, InventoryEventType type, UUID productId, long quantityDelta);

  /** Processes one batch of due events. Returns how many were handled. */
  int processPendingBatch();
}
