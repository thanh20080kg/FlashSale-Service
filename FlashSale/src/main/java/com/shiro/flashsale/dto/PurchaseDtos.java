package com.shiro.flashsale.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public final class PurchaseDtos {
  private PurchaseDtos() {}

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PurchaseRequest {
    @NotNull private UUID itemId;
    private Integer quantity;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class PurchaseResponse {
    private final UUID purchaseId;
    private final UUID itemId;
    private final String sku;
    private final BigDecimal amount;
    private final LocalDate purchaseDate;
    private final String status;
    private final String message;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class PurchaseHistoryResponse {
    private final UUID purchaseId;
    private final UUID itemId;
    private final String sku;
    private final String productName;
    private final BigDecimal amount;
    private final LocalDate purchaseDate;
    private final String status;
    private final Instant createdAt;
  }
}
