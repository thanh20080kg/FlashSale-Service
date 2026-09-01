package com.shiro.warehouse.client;

import com.shiro.warehouse.config.AppProperties;
import com.shiro.warehouse.dto.PurchaseStatusSyncDtos;
import com.shiro.warehouse.messaging.raditmq.RabbitProducer;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlashSaleClient {
  private final RabbitProducer rabbitProducer;
  private final AppProperties properties;

  public String getPurchasesStatus(List<UUID> request) throws Exception {
    return rabbitProducer.send(
        properties.getRabbitQueue().getStatusSyncQueue(),
        new PurchaseStatusSyncDtos.Request(request));
  }

  public String getPurchasesStatus(List<UUID> request, int retry) throws Exception {
    return rabbitProducer.send(
        properties.getRabbitQueue().getStatusSyncQueue(),
        new PurchaseStatusSyncDtos.Request(request),
        retry);
  }
}
