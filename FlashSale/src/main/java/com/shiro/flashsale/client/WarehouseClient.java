package com.shiro.flashsale.client;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.dto.client.WarehouseDtos;
import com.shiro.flashsale.messaging.rabit.RabbitProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarehouseClient {
  private final RabbitProducer rabbitProducer;
  private final AppProperties properties;

  public String send(WarehouseDtos.Request request) throws Exception {
    return rabbitProducer.send(properties.getWarehouse().getQueue(), request);
  }

  public String send(WarehouseDtos.Request request, int retry) throws Exception {
    return rabbitProducer.send(properties.getWarehouse().getQueue(), request, retry);
  }
}
