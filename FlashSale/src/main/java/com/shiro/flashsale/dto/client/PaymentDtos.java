package com.shiro.flashsale.dto.client;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Request {
    private Operation operation;
    private UUID purchaseId;
    private UUID payerAccountId;
    private UUID payeeAccountId;
    private BigDecimal amount;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class Response {
    private final boolean success;
    private final UUID purchaseId;
    private final UUID transactionId;
    private final PaymentStatus status;
    private final String message;
  }
}
