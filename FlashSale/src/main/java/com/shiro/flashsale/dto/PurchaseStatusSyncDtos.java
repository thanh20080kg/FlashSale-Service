package com.shiro.flashsale.dto;

import java.util.List;
import java.util.UUID;

public final class PurchaseStatusSyncDtos {
  private PurchaseStatusSyncDtos() {}

  public record Request(List<UUID> purchaseIds) {}

  public record Entry(UUID purchaseId, String status) {}

  public record Response(List<Entry> purchases) {}
}
