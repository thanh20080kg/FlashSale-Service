package com.shiro.flashsale.messaging.kafka;

import com.shiro.flashsale.service.MaintenanceService;
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
  private final MaintenanceService maintenanceService;
  private final SaleService saleService;

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
    maintenanceService.reload();
    log.info("Maintenance configuration reloaded successfully");
  }
}
