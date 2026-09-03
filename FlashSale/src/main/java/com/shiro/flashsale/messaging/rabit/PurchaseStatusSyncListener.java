package com.shiro.flashsale.messaging.rabit;

import com.shiro.flashsale.dto.PurchaseStatusSyncDtos;
import com.shiro.flashsale.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PurchaseStatusSyncListener {
  private static final Logger log = LoggerFactory.getLogger(PurchaseStatusSyncListener.class);
  private final PurchaseService purchaseService;
  private final ObjectMapper objectMapper;

  @RabbitListener(queues = "${app.warehouse.status-sync-queue}")
  public String getStatus(String payload) {
    log.info(
        "RABBIT_CONSUMER_IN consumer={} payload={}",
        "PurchaseStatusSyncListener.getStatus",
        payload);

    PurchaseStatusSyncDtos.Request request =
        objectMapper.readValue(payload, PurchaseStatusSyncDtos.Request.class);
    String response =
        objectMapper.writeValueAsString(purchaseService.getStatus(request.purchaseIds()));

    log.info(
        "RABBIT_CONSUMER_OUT consumer={} response={}",
        "PurchaseStatusSyncListener.getStatus",
        response);
    return response;
  }
}
