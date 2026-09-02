package com.shiro.warehouse.constants;

public final class ServiceConstants {
  public static final String INVALID_QUANTITY_MESSAGE = "quantity must be greater than zero";
  public static final String INVENTORY_NOT_FOUND_WHILE_RELEASING =
      "Inventory was not found while releasing reservation";
  public static final String INVENTORY_NOT_FOUND_WHILE_CONFIRMING =
      "Inventory was not found while confirming reservation";
  private ServiceConstants() {}
}
