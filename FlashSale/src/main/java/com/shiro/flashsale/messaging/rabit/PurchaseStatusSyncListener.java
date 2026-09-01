package com.shiro.flashsale.messaging.rabit;

import com.shiro.flashsale.dto.PurchaseStatusSyncDtos;
import com.shiro.flashsale.service.PurchaseStatusSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PurchaseStatusSyncListener {
  private final PurchaseStatusSyncService syncService;
  private final ObjectMapper objectMapper;

  @RabbitListener(queues = "${app.warehouse.status-sync-queue}")
  public String reply(String payload) throws Exception {
    PurchaseStatusSyncDtos.Request request =
        objectMapper.readValue(payload, PurchaseStatusSyncDtos.Request.class);
    return objectMapper.writeValueAsString(syncService.statuses(request.purchaseIds()));
  }
}
