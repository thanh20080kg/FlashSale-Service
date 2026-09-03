package com.shiro.warehouse.messaging.raditmq;

import static com.shiro.warehouse.dto.WarehouseDtos.Status.*;

import com.shiro.warehouse.dto.WarehouseDtos;
import com.shiro.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RabitEventListener {
  private final Logger logger = LoggerFactory.getLogger(RabitEventListener.class);
  private final WarehouseService service;
  private final ObjectMapper objectMapper;

  @Value("${app.rabbit-queue.inventory-queue:warehouse.inventory.commands}")
  private String INVENTORY_QUEUE;

  @RabbitListener(queues = "${app.rabbit-queue.inventory-queue:warehouse.inventory.commands}")
  public String handle(String payload) {
    String response;
    try {
      logger.info(
          "RABBIT_CONSUMER_IN consumer=RabitEventListener.handle queue={} payload={}",
          INVENTORY_QUEUE,
          payload);
      WarehouseDtos.Request command = objectMapper.readValue(payload, WarehouseDtos.Request.class);
      WarehouseDtos.Response result =
          switch (command.operation()) {
            case RESERVE -> service.reserve(command);
            case RELEASE -> service.release(command);
            case CONFIRM -> service.sold(command);
          };

      response = objectMapper.writeValueAsString(result);
    } catch (Exception exception) {
      logger.error(
          "RABBIT_CONSUMER_ERROR consumer=RabitEventListener.handle queue={} message={}",
          INVENTORY_QUEUE,
          exception.getMessage(),
          exception);
      response =
          objectMapper.writeValueAsString(
              new WarehouseDtos.Response(false, INVALID, "invalid JSON command", null, null));
    }
    logger.info(
        "RABBIT_CONSUMER_OUT consumer=RabitEventListener.handle queue={} response={}",
        INVENTORY_QUEUE,
        response);
    return response;
  }
}
