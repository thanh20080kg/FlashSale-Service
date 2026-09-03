package com.shiro.warehouse.messaging.kafka;

import com.shiro.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseStatusSyncTriggerListener {
  private static final Logger log =
      LoggerFactory.getLogger(PurchaseStatusSyncTriggerListener.class);
  private final WarehouseService warehouseService;

  @KafkaListener(topics = "${app.kafka-topic.trigger-status-sync}")
  public void onSyncTrigger(@Payload(required = false) String ignoredPayload) {
    log.info(
        "KAFKA_CONSUMER_IN consumer=PurchaseStatusSyncTriggerListener.onSyncTrigger payload={}",
        ignoredPayload);
    warehouseService.syncPurchaseStatuses();
    log.info(
        "KAFKA_CONSUMER_OUT consumer=PurchaseStatusSyncTriggerListener.onSyncTrigger result=purchase statuses synchronized");
  }
}
