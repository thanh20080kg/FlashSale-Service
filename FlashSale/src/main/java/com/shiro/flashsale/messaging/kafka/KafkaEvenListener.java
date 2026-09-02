package com.shiro.flashsale.messaging.kafka;

import com.shiro.flashsale.service.PaymentStatusSyncService;
import com.shiro.flashsale.service.ReloadConfigService;
import com.shiro.flashsale.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEvenListener {
  private static final Logger log = LoggerFactory.getLogger(KafkaEvenListener.class);
  private final ReloadConfigService reloadConfigService;
  private final SaleService saleService;
  private final PaymentStatusSyncService paymentStatusSyncService;

  @EventListener(ApplicationReadyEvent.class)
  @KafkaListener(topics = "${app.kafka-topic.quota-reload-topic}")
  public void reloadQuotaTrigger() {
    log.info("Start trigger quota reload");
    saleService.reloadQuota();
    log.info("End trigger quota reload");
  }

  @KafkaListener(topics = "${app.kafka-topic.trigger-reload}")
  public void onReloadConfigTrigger() {
    log.info("Received maintenance configuration reload trigger");
    reloadConfigService.reload();
    log.info("Maintenance configuration reloaded successfully");
  }

  @KafkaListener(topics = "${app.kafka-topic.payment-status-sync}")
  public void onPaymentStatusSyncTrigger() {
    log.info("Received payment status sync trigger");
    paymentStatusSyncService.syncPending();
  }
}
