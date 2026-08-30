package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.service.InventorySyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drains the inventory outbox on a fixed delay. Safe to run on every instance simultaneously - see
 * {@link InventorySyncServiceImpl} for why.
 */
@Component
@ConditionalOnProperty(name = "app.inventory-sync.enabled", havingValue = "true")
public class InventorySyncWorker {
  private static final Logger log = LoggerFactory.getLogger(InventorySyncWorker.class);

  private final InventorySyncService syncService;

  public InventorySyncWorker(InventorySyncService syncService) {
    this.syncService = syncService;
  }

  @Scheduled(
      fixedDelayString = "${app.inventory-sync.interval}",
      initialDelayString = "${app.inventory-sync.interval}")
  public void drainOutbox() {
    try {
      int processed = syncService.processPendingBatch();
      if (processed > 0) {
        log.debug("Inventory sync processed {} event(s)", processed);
      }
    } catch (RuntimeException ex) {
      // Never let a failing tick kill the scheduler; the next tick re-claims the same rows.
      log.error("Inventory sync batch failed", ex);
    }
  }
}
