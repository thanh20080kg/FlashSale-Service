package com.shiro.flashsale.constants;

public enum InventoryEventType {
  /** A flash sale purchase reserved one unit. */
  PURCHASE_RESERVED,
  /** An operator changed the on-hand stock of a product. */
  STOCK_ADJUSTED,
  /** A product was deactivated; today's flash sale quota must be closed. */
  PRODUCT_DEACTIVATED
}
