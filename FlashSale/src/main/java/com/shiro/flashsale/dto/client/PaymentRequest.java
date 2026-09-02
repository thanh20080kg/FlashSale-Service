package com.shiro.flashsale.dto.client;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
    Operation operation,
    UUID purchaseId,
    UUID payerAccountId,
    UUID payeeAccountId,
    BigDecimal amount) {
  public enum Operation {
    PENDING,
    CONFIRM,
    CANCEL,
    STATUS
  }
}
