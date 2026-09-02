package com.shiro.payment.messaging;

import com.shiro.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PaymentCommandListener {
  private final PaymentService service;
  private final ObjectMapper objectMapper;

  @RabbitListener(queues = "${app.rabbit-queue.payment-command}")
  public String handle(String payload) {
    try {
      PaymentDtos.Request request = objectMapper.readValue(payload, PaymentDtos.Request.class);
      PaymentDtos.Response response =
          switch (request.operation()) {
            case PENDING ->
                service.pending(
                    request.purchaseId(),
                    request.payerAccountId(),
                    request.payeeAccountId(),
                    request.amount());
            case CONFIRM -> service.confirm(request.purchaseId());
            case CANCEL -> service.cancel(request.purchaseId());
            case STATUS -> service.status(request.purchaseId());
          };
      return objectMapper.writeValueAsString(response);
    } catch (Exception exception) {
      return objectMapper.writeValueAsString(
          new PaymentDtos.Response(false, null, null, "FAILED", exception.getMessage()));
    }
  }
}
