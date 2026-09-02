package com.shiro.flashsale.exception;

public class PurchaseResponseException extends RuntimeException {
  private final Object object;

  public PurchaseResponseException(Object object) {
    this.object = object;
  }

  public Object getObject() {
    return object;
  }
}
