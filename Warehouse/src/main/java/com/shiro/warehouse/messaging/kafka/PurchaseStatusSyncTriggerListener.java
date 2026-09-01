package com.shiro.warehouse.messaging.kafka;

import com.shiro.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseStatusSyncTriggerListener {
  private final WarehouseService warehouseService;

  @KafkaListener(topics = "${app.kafka-topic.trigger-status-sync}")
  public void onSyncTrigger(@Payload(required = false) String ignoredPayload) {
    warehouseService.syncPurchaseStatuses();
  }
}
