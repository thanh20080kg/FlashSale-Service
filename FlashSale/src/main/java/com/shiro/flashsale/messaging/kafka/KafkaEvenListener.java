package com.shiro.flashsale.messaging.kafka;

import com.shiro.flashsale.service.CacheConfigService;
import com.shiro.flashsale.service.PaymentStatusSyncService;
import com.shiro.flashsale.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEvenListener {
  private static final Logger log = LoggerFactory.getLogger(KafkaEvenListener.class);
  private final CacheConfigService cacheConfigService;
  private final PurchaseService purchaseService;
  private final PaymentStatusSyncService paymentStatusSyncService;

  @KafkaListener(topics = "${app.kafka-topic.quota-reload-topic}")
  public void reloadQuotaTrigger(@Payload(required = false) String payload) {
    log.info(
        "KAFKA_CONSUMER_IN consumer={} payload={}",
        "KafkaEvenListener.reloadQuotaTrigger",
        payload);

    purchaseService.reloadQuota();

    log.info(
        "KAFKA_CONSUMER_OUT consumer={} result={}",
        "KafkaEvenListener.reloadQuotaTrigger",
        "quota reloaded");
  }

  @KafkaListener(topics = "${app.kafka-topic.trigger-reload}")
  public void onReloadConfigTrigger(@Payload(required = false) String payload) {
    log.info(
        "KAFKA_CONSUMER_IN consumer={} payload={}",
        "KafkaEvenListener.onReloadConfigTrigger",
        payload);

    cacheConfigService.reload();

    log.info(
        "KAFKA_CONSUMER_OUT consumer={} result={}",
        "KafkaEvenListener.onReloadConfigTrigger",
        "configuration reloaded");
  }

  @KafkaListener(topics = "${app.kafka-topic.payment-status-sync}")
  public void onPaymentStatusSyncTrigger(@Payload(required = false) String payload) {
    log.info(
        "KAFKA_CONSUMER_IN consumer={} payload={}",
        "KafkaEvenListener.onPaymentStatusSyncTrigger",
        payload);

    paymentStatusSyncService.syncPending();

    log.info(
        "KAFKA_CONSUMER_OUT consumer={} result={}",
        "KafkaEvenListener.onPaymentStatusSyncTrigger",
        "pending payments synchronized");
  }
}
