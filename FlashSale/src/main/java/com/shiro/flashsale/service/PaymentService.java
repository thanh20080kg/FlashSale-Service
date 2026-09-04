package com.shiro.flashsale.service;

import com.shiro.flashsale.client.PaymentClient;
import com.shiro.flashsale.constants.ServiceConstants;
import com.shiro.flashsale.dto.client.PaymentDtos;
import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import com.shiro.flashsale.repository.PurchaseRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class PaymentService {
  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
  private final PaymentClient client;
  private final ObjectMapper objectMapper;
  private final WarehouseService warehouse;
  private final PurchaseRepository purchases;
  private final FlashSaleItemQuotaRepository quotas;

  public PaymentDtos.Response pending(
      UUID purchaseId, UUID payerAccountId, UUID payeeAccountId, BigDecimal amount) {
    return send(
        PaymentDtos.Request.builder()
            .operation(PaymentDtos.Operation.PENDING)
            .purchaseId(purchaseId)
            .payerAccountId(payerAccountId)
            .payeeAccountId(payeeAccountId)
            .amount(amount)
            .build());
  }

  public PaymentDtos.Response confirm(UUID purchaseId) {
    return send(
        PaymentDtos.Request.builder()
            .operation(PaymentDtos.Operation.CONFIRM)
            .purchaseId(purchaseId)
            .build());
  }

  public PaymentDtos.Response cancel(UUID purchaseId) {
    return send(
        PaymentDtos.Request.builder()
            .operation(PaymentDtos.Operation.CANCEL)
            .purchaseId(purchaseId)
            .build());
  }

  public PaymentDtos.Response getStatus(UUID purchaseId) {
    return send(
        PaymentDtos.Request.builder()
            .operation(PaymentDtos.Operation.STATUS)
            .purchaseId(purchaseId)
            .build());
  }

  private PaymentDtos.Response send(PaymentDtos.Request request) {
    try {
      return objectMapper.readValue(client.send(request), PaymentDtos.Response.class);
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
