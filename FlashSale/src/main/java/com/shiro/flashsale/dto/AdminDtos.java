package com.shiro.flashsale.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public final class AdminDtos {
  private AdminDtos() {}

  // ---- products ----

  public record CreateProductRequest(
      @NotBlank @Size(max = 64) String sku,
      @NotBlank @Size(max = 200) String name,
      @NotNull @PositiveOrZero Long initialStock) {}

  public record UpdateProductRequest(@Size(max = 200) String name, Boolean active) {}

  public record ProductResponse(
      UUID id,
      String sku,
      String name,
      boolean active,
      long availableQuantity,
      long soldQuantity) {}

  // ---- inventory ----

  public record AdjustInventoryRequest(
      @NotNull Long delta, @NotBlank @Size(max = 150) String reference) {}

  public record InventoryMovementResponse(
      UUID id,
      UUID productId,
      String eventType,
      long quantityDelta,
      long balanceAfter,
      Instant createdAt) {}

  // ---- slots ----

  public record CreateSlotRequest(
      @NotBlank @Size(max = 100) String name,
      @NotNull LocalTime startTime,
      @NotNull LocalTime endTime) {}

  public record UpdateSlotRequest(Boolean active) {}

  public record SlotResponse(
      UUID id,
      String name,
      LocalTime startTime,
      LocalTime endTime,
      boolean active,
      boolean overnight) {}

  // ---- flash sale items ----

  public record CreateItemRequest(
      @NotNull UUID productId,
      @NotNull @DecimalMin("0.00") BigDecimal amount,
      @NotNull @Positive Long quantity) {}

  public record UpdateItemRequest(
      @DecimalMin("0.00") BigDecimal amount, @Positive Long quantity, Boolean active) {}

  public record ItemResponse(
      UUID id,
      UUID slotId,
      String slotName,
      UUID productId,
      String sku,
      BigDecimal amount,
      long quantity,
      boolean active) {}

  // ---- balance ----

  public record TopUpRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal amount) {}

  public record CustomerResponse(
      UUID customerId, UUID userId, String displayName, BigDecimal balance) {}

  // ---- ops ----

  public record SyncStatusResponse(long pending, long processed, long failed) {}
}
