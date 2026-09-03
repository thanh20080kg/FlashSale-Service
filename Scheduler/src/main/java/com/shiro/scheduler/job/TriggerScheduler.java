package com.shiro.scheduler.job;

import com.shiro.scheduler.messaging.kafka.KafkaTriggerProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Publishes scheduled synchronization commands to downstream services. */
@Component
@RequiredArgsConstructor
public class TriggerScheduler {
  private static final Logger log = LoggerFactory.getLogger(TriggerScheduler.class);

  private final KafkaTriggerProducer kafkaTriggerProducer;

  @Value("${app.kafka-topic.quota-reload-topic}")
  private String quotaReloadTopic;

  @Value("${app.kafka-topic.trigger-status-sync}")
  private String statusSyncTopic;

  @Value("${app.kafka-topic.payment-status-sync}")
  private String paymentStatusSyncTopic;

  @Scheduled(cron = "${app.scheduler.quota-reload-cron}", zone = "${app.timezone}")
  public void onReloadQuotaTrigger() {
    log.info("Publishing quota reload trigger");
    kafkaTriggerProducer.send(quotaReloadTopic);
  }

  @Scheduled(cron = "${app.scheduler.status-sync-cron}", zone = "${app.timezone}")
  public void onStatusSyncTrigger() {
    log.info("Publishing purchase status sync trigger");
    kafkaTriggerProducer.send(statusSyncTopic);
  }

  @Scheduled(cron = "${app.scheduler.payment-status-sync-cron}", zone = "${app.timezone}")
  public void onPaymentStatusSyncTrigger() {
    log.info("Publishing payment status sync trigger");
    kafkaTriggerProducer.send(paymentStatusSyncTopic);
  }
}
