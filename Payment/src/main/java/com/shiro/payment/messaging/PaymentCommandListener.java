package com.shiro.payment.messaging;

import com.shiro.payment.dto.PaymentDtos;
import com.shiro.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PaymentCommandListener {
  private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);
  private final PaymentService service;
  private final ObjectMapper objectMapper;

  @RabbitListener(queues = "${app.rabbit-queue.payment-command}")
  public String handle(String payload) {
    log.info("RABBIT_CONSUMER_IN consumer={} payload={}", "PaymentCommandListener.handle", payload);
    String responsePayload;
    try {
      PaymentDtos.Request request = objectMapper.readValue(payload, PaymentDtos.Request.class);
      PaymentDtos.Response response =
          switch (request.getOperation()) {
            case PENDING ->
                service.pending(
                    request.getPurchaseId(),
                    request.getPayerAccountId(),
                    request.getPayeeAccountId(),
                    request.getAmount());
            case CONFIRM -> service.confirm(request.getPurchaseId());
            case CANCEL -> service.cancel(request.getPurchaseId());
            case STATUS -> service.status(request.getPurchaseId());
          };
      responsePayload = objectMapper.writeValueAsString(response);
    } catch (Exception exception) {
      log.error("RABBIT_CONSUMER_OUT consumer=PaymentCommandListener.handle exception=", exception);
      responsePayload =
          objectMapper.writeValueAsString(
              new PaymentDtos.Response(false, null, null, "FAILED", exception.getMessage()));
    }

    log.info(
        "RABBIT_CONSUMER_OUT consumer=PaymentCommandListener.handle response={}", responsePayload);
    return responsePayload;
  }
}
