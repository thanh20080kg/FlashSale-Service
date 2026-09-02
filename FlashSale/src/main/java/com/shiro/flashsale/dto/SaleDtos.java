package com.shiro.flashsale.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class SaleDtos {
  private SaleDtos() {}

  public record SaleItemResponse(
      UUID itemId,
      UUID productId,
      String sku,
      String productName,
      BigDecimal amount,
      long quantity,
      long remainingQuantity,
      UUID slotId,
      String slotName,
      LocalTime startTime,
      LocalTime endTime,
      boolean overnight) {}

  public record PurchaseRequest(@NotNull UUID itemId) {}

  public record PurchaseResponse(
      UUID purchaseId,
      UUID itemId,
      String sku,
      BigDecimal amount,
      LocalDate purchaseDate,
      String status,
      String message) {}

  public record PurchaseHistoryResponse(
      UUID purchaseId,
      UUID itemId,
      String sku,
      String productName,
      BigDecimal amount,
      LocalDate purchaseDate,
      String status,
      Instant createdAt) {}

  public record BalanceResponse(UUID customerId, String displayName, BigDecimal balance) {}
}
