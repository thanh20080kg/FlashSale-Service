package com.shiro.authentication.message.kafka;

import com.shiro.authentication.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfigReloadListener {
  private static final Logger log = LoggerFactory.getLogger(ConfigReloadListener.class);
  private final MaintenanceService maintenanceService;

  @KafkaListener(topics = "${app.kafka.topic.maintenance}")
  public void onReloadTrigger(@Payload(required = false) String ignoredPayload) {
    log.info("Received maintenance configuration reload trigger");
    maintenanceService.reload();
    log.info("Maintenance configuration reloaded successfully");
  }
}
