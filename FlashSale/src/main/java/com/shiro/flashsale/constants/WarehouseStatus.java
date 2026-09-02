package com.shiro.flashsale.constants;

public enum WarehouseStatus {
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

  WarehouseStatus(String defaultMessage) {
    this.defaultMessage = defaultMessage;
  }

  public static WarehouseStatus fromString(String status) {
    for (WarehouseStatus warehouseStatus : WarehouseStatus.values()) {
      if (warehouseStatus.name().equalsIgnoreCase(status)) {
        return warehouseStatus;
      }
    }
    return INVALID;
  }
}
