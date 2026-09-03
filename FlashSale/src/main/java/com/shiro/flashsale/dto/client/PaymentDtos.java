package com.shiro.flashsale.dto.client;

import java.math.BigDecimal;
import java.util.UUID;

public final class PaymentDtos {
  private PaymentDtos() {}

  public enum Operation {
    PENDING,
    CONFIRM,
    CANCEL,
    STATUS
  }

  public enum PaymentStatus {
    PENDING,
    COMPLETE,
    FAILED,
    CANCELLED
  }

  public record Request(
      Operation operation,
      UUID purchaseId,
      UUID payerAccountId,
      UUID payeeAccountId,
      BigDecimal amount) {}

  public record Response(
      boolean success, UUID purchaseId, UUID transactionId, PaymentStatus status, String message) {}
}
