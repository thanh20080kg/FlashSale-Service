package com.shiro.flashsale.service;

import com.shiro.flashsale.client.PaymentClient;
import com.shiro.flashsale.constants.ServiceConstants;
import com.shiro.flashsale.dto.client.PaymentRequest;
import com.shiro.flashsale.dto.client.PaymentResult;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class PaymentService {
  private final PaymentClient client;
  private final ObjectMapper objectMapper;

  public PaymentResult pending(
      UUID purchaseId, UUID payerAccountId, UUID payeeAccountId, BigDecimal amount) {
    return send(
        new PaymentRequest(
            PaymentRequest.Operation.PENDING, purchaseId, payerAccountId, payeeAccountId, amount));
  }

  public PaymentResult confirm(UUID purchaseId) {
    return send(new PaymentRequest(PaymentRequest.Operation.CONFIRM, purchaseId, null, null, null));
  }

  public PaymentResult cancel(UUID purchaseId) {
    return send(new PaymentRequest(PaymentRequest.Operation.CANCEL, purchaseId, null, null, null));
  }

  public PaymentResult status(UUID purchaseId) {
    return send(new PaymentRequest(PaymentRequest.Operation.STATUS, purchaseId, null, null, null));
  }

  private PaymentResult send(PaymentRequest request) {
    try {
      return objectMapper.readValue(client.send(request), PaymentResult.class);
    } catch (Exception exception) {
      throw new PaymentCommunicationException(
          ServiceConstants.PAYMENT_SERVICE_UNAVAILABLE, exception);
    }
  }

  public static class PaymentCommunicationException extends RuntimeException {
    public PaymentCommunicationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
