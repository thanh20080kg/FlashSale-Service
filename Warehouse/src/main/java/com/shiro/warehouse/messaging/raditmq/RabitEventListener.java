package com.shiro.warehouse.messaging.raditmq;

import static com.shiro.warehouse.constants.OrderStatus.INVALID;

import com.shiro.warehouse.dto.WarehouseRequest;
import com.shiro.warehouse.dto.WarehouseResponse;
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
      logger.info("Received queue: {} payload: {}", INVENTORY_QUEUE, payload);
      WarehouseRequest command = objectMapper.readValue(payload, WarehouseRequest.class);
      WarehouseResponse result =
          switch (command.operation()) {
            case RESERVED -> service.reserve(command);
            case RELEASED -> service.release(command);
            case SOLD -> service.sold(command);
          };

      String response = objectMapper.writeValueAsString(result);
      logger.info("Received queue: {} reponse: {}", INVENTORY_QUEUE, response);
      return response;
    } catch (Exception exception) {
      logger.error("Error processing inventory command: {}", exception.getMessage());
      return objectMapper.writeValueAsString(
          new WarehouseResponse(false, INVALID, "invalid JSON command"));
    }
  }
}
