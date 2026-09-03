package com.shiro.flashsale.dto.client;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public final class WarehouseDtos {
  private WarehouseDtos() {}

  public enum Operation {
    RESERVE,
    CONFIRM,
    RELEASE
  }

  public enum Status {
    RESERVED("reserved"),
    SOLD("sold"),
    RELEASED("released"),
    ALREADY_RESERVED("idempotent"),
    ALREADY_SOLD("idempotent"),
    ALREADY_RELEASED("idempotent"),
    OUT_OF_STOCK("out of stock"),
    NOT_EXIST("not exist"),
    INVALID("unknown operation");

    public final String defaultMessage;

    Status(String defaultMessage) {
      this.defaultMessage = defaultMessage;
    }

    public static Status fromString(String string) {
      for (Status status : Status.values()) {
        if (status.name().equalsIgnoreCase(string)) {
          return status;
        }
      }
      return INVALID;
    }
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Request {
    private Operation operation;
    private UUID reservationKey;
    private UUID productId;
    private Integer quantity;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class Response {
    private final boolean success;
    private final Status status;
    private final String message;
    private final UUID productId;
    private final Integer quantity;
  }
}
