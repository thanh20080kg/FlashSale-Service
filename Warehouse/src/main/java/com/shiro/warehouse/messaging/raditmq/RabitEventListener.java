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
    try {
      logger.info(
          "RABBIT_CONSUMER_IN consumer={} queue={} payload={}",
          "RabitEventListener.handle",
          INVENTORY_QUEUE,
          payload);
      WarehouseDtos.Request command = objectMapper.readValue(payload, WarehouseDtos.Request.class);
      WarehouseDtos.Response result =
          switch (command.operation()) {
            case RESERVE -> service.reserve(command);
            case RELEASE -> service.release(command);
            case CONFIRM -> service.sold(command);
          };

      String response = objectMapper.writeValueAsString(result);
      logger.info(
          "RABBIT_CONSUMER_OUT consumer={} queue={} response={}",
          "RabitEventListener.handle",
          INVENTORY_QUEUE,
          response);
      return response;
    } catch (Exception exception) {
      logger.error(
          "RABBIT_CONSUMER_ERROR consumer={} queue={} message={}",
          "RabitEventListener.handle",
          INVENTORY_QUEUE,
          exception.getMessage(),
          exception);
      String response =
          objectMapper.writeValueAsString(
              new WarehouseDtos.Response(false, INVALID, "invalid JSON command", null, null));
      logger.info(
          "RABBIT_CONSUMER_OUT consumer={} queue={} response={}",
          "RabitEventListener.handle",
          INVENTORY_QUEUE,
          response);
      return response;
    }
  }
}
