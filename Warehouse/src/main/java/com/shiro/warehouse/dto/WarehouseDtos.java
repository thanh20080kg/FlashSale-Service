package com.shiro.warehouse.dto;

import java.util.UUID;

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

  public record Request(
      Operation operation, UUID reservationKey, UUID productId, Integer quantity) {}

  public record Response(
      boolean success, Status status, String message, UUID productId, Integer quantity) {}
}
