package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.constants.InventoryEventType;
import com.shiro.flashsale.entity.Inventory;
import com.shiro.flashsale.entity.InventoryEvent;
import com.shiro.flashsale.entity.InventoryMovement;
import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import com.shiro.flashsale.repository.FlashSaleItemRepository;
import com.shiro.flashsale.repository.InventoryEventRepository;
import com.shiro.flashsale.repository.InventoryMovementRepository;
import com.shiro.flashsale.repository.InventoryRepository;
import com.shiro.flashsale.service.InventorySyncService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class InventorySyncServiceImpl implements InventorySyncService {
  private static final Logger log = LoggerFactory.getLogger(InventorySyncServiceImpl.class);

  private final InventoryEventRepository events;
  private final InventoryMovementRepository movements;
  private final InventoryRepository inventory;
  private final FlashSaleItemRepository items;
  private final FlashSaleItemQuotaRepository quotas;
  private final AppProperties properties;

  @Override
  public void enqueue(
      String eventKey, InventoryEventType type, UUID productId, long quantityDelta) {
    events.save(new InventoryEvent(eventKey, type, productId, quantityDelta, Instant.now()));
  }

  @Override
  @Transactional
  public int processPendingBatch() {
    Instant now = Instant.now();
    List<InventoryEvent> batch =
        events.claimPending(now, properties.getInventorySync().getBatchSize());
    if (batch.isEmpty()) {
      return 0;
    }

    for (InventoryEvent event : batch) {
      try {
        apply(event, now);
        event.markProcessed(now);
      } catch (DataIntegrityViolationException alreadyApplied) {
        // The ledger's unique key rejected a replay. The desired end state already holds.
        log.warn("Inventory event {} was already applied, marking processed", event.getEventKey());
        event.markProcessed(now);
      } catch (RuntimeException ex) {
        handleFailure(event, now, ex);
      }
    }
    events.saveAll(batch);
    return batch.size();
  }

  private void apply(InventoryEvent event, Instant now) {
    switch (event.getEventType()) {
      case ORDER_SOLD -> {
        // available_quantity was already decremented inside the purchase transaction (that is what
        // prevents overselling). The worker maintains the derived counters and the audit ledger.
        inventory.addSold(event.getProductId(), -event.getQuantityDelta());
        recordMovement(event, now);
      }
      case STOCK_ADJUSTED -> recordMovement(event, now);
      case PRODUCT_DEACTIVATED -> {
        List<UUID> itemIds = items.findIdsByProductId(event.getProductId());
        if (!itemIds.isEmpty()) {
          int closed = quotas.closeQuotas(itemIds, LocalDate.now());
          log.info(
              "Product {} deactivated: closed {} flash sale quota row(s) for today",
              event.getProductId(),
              closed);
        }
        recordMovement(event, now);
      }
    }
  }

  private void recordMovement(InventoryEvent event, Instant now) {
    long balanceAfter =
        inventory
            .findByProductId(event.getProductId())
            .map(Inventory::getAvailableQuantity)
            .orElse(0L);
    movements.saveAndFlush(
        new InventoryMovement(
            event.getId(),
            event.getProductId(),
            event.getEventType(),
            event.getQuantityDelta(),
            balanceAfter,
            now));
  }

  private void handleFailure(InventoryEvent event, Instant now, RuntimeException ex) {
    int maxAttempts = properties.getInventorySync().getMaxAttempts();
    if (event.getAttempts() + 1 >= maxAttempts) {
      log.error(
          "Inventory event {} exhausted {} attempts, parking as FAILED",
          event.getEventKey(),
          maxAttempts,
          ex);
      event.markFailed(now, ex.getMessage());
      return;
    }
    log.warn("Inventory event {} failed, will retry: {}", event.getEventKey(), ex.getMessage());
    event.markRetry(now.plus(properties.getInventorySync().getRetryBackoff()), ex.getMessage());
  }
}
