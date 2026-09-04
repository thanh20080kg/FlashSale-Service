package com.shiro.flashsale.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SaleItemResponse {
  private final UUID itemId;
  private final UUID productId;
  private final String sku;
  private final String productName;
  private final BigDecimal amount;
  private final long quantity;
  private final long remainingQuantity;
  private final UUID slotId;
  private final String slotName;
  private final LocalTime startTime;
  private final LocalTime endTime;
  private final boolean overnight;
}
