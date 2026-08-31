package com.shiro.authentication.message;

import com.shiro.authentication.service.MaintenanceConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceReloadListener {
  private static final Logger log = LoggerFactory.getLogger(MaintenanceReloadListener.class);
  private final MaintenanceConfigService maintenanceConfig;

  public MaintenanceReloadListener(MaintenanceConfigService maintenanceConfig) {
    this.maintenanceConfig = maintenanceConfig;
  }

  @KafkaListener(
      topics = "${app.kafka.topic.maintenance}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "kafkaListenerContainerFactory")
  public void onReloadTrigger(@Payload(required = false) String ignoredPayload) {
    log.info("Received maintenance configuration reload trigger");
    maintenanceConfig.reload();
    log.info("Maintenance configuration reloaded successfully");
  }
}
