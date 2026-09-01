package com.shiro.warehouse.messaging.raditmq;

import java.util.Objects;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Sends request/reply messages. Business response interpretation belongs to the client. */
@Component
@AllArgsConstructor
public class RabbitProducer {

  private static final Logger log = LoggerFactory.getLogger(RabbitProducer.class);

  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public String send(String queue, Object payload) throws Exception {
    return send(queue, payload, 0);
  }

  public String send(String queue, Object payload, int retry) throws Exception {
    int maxAttempts = retry > 0 ? retry + 1 : 1;
    Exception lastFailure = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        String request = objectMapper.writeValueAsString(payload);
        log.info("RabbitMQ queue: {} request: {}", queue, request);

        Object response = rabbitTemplate.convertSendAndReceive(queue, request);
        log.info("RabbitMQ queue: {} res: {}", queue, objectMapper.writeValueAsString(response));

        if (response instanceof String responseBody && !responseBody.isBlank()) {
          return responseBody;
        }
        throw new RabbitProducerException("RabbitMQ returned no response");
      } catch (Exception exception) {
        lastFailure = exception;
        if (attempt < maxAttempts) {
          try {
            Thread.sleep(100L * attempt);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new RabbitProducerException("RabbitMQ retry interrupted", interrupted);
          }
        }
      }
    }
    throw Objects.requireNonNull(lastFailure);
  }

  public static class RabbitProducerException extends RuntimeException {
    public RabbitProducerException(String message) {
      super(message);
    }

    public RabbitProducerException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
