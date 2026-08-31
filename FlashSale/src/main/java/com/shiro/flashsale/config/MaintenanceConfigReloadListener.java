package com.shiro.flashsale.config;

import com.shiro.flashsale.service.MaintenanceConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceConfigReloadListener {
  private static final Logger log = LoggerFactory.getLogger(MaintenanceConfigReloadListener.class);
  private final MaintenanceConfigService maintenanceConfig;

  public MaintenanceConfigReloadListener(MaintenanceConfigService maintenanceConfig) {
    this.maintenanceConfig = maintenanceConfig;
  }

  @KafkaListener(
      topics = "${app.maintenance.kafka-topic}",
      groupId = "${app.maintenance.kafka-group-id}")
  public void onReloadTrigger(@Payload(required = false) String ignoredPayload) {
    log.info("Received maintenance configuration reload trigger");
    maintenanceConfig.reload();
    log.info("Maintenance configuration reloaded successfully");
  }
}
