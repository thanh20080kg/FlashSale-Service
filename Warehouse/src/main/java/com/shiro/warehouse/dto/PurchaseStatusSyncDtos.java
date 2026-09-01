package com.shiro.warehouse.dto;

import com.shiro.warehouse.constants.PurchaseStatus;
import java.util.List;
import java.util.UUID;

public final class PurchaseStatusSyncDtos {
  private PurchaseStatusSyncDtos() {}

  public record Request(List<UUID> purchaseIds) {}

  public record Entry(UUID purchaseId, PurchaseStatus status) {}

  public record Response(List<Entry> purchases) {}
}
