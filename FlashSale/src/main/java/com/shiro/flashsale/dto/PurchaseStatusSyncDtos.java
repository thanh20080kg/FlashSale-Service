package com.shiro.flashsale.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public final class PurchaseStatusSyncDtos {
  private PurchaseStatusSyncDtos() {}

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Request {
    private List<UUID> purchaseIds;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class Entry {
    private final String status;
    private UUID purchaseId;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class Response {
    private List<Entry> purchases;
  }
}
