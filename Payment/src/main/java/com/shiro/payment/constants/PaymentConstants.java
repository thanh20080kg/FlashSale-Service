package com.shiro.payment.constants;

public final class PaymentConstants {
  public static final String SAME_PAYMENT_ACCOUNT = "SAME_PAYMENT_ACCOUNT";
  public static final String PAYEE_ACCOUNT_NOT_FOUND_OR_INACTIVE =
      "PAYEE_ACCOUNT_NOT_FOUND_OR_INACTIVE";
  public static final String PAYER_ACCOUNT_INACTIVE_OR_INSUFFICIENT_FUNDS =
      "PAYER_ACCOUNT_INACTIVE_OR_INSUFFICIENT_FUNDS";
  public static final String PAYER_CAPTURE_FAILED = "PAYER_CAPTURE_FAILED";
  public static final String PAYEE_CREDIT_FAILED = "PAYEE_CREDIT_FAILED";
  public static final String TRANSACTION_COMPLETE_FAILED = "TRANSACTION_COMPLETE_FAILED";
  public static final String PAYER_RELEASE_FAILED = "PAYER_RELEASE_FAILED";
  public static final String TRANSACTION_CANCEL_FAILED = "TRANSACTION_CANCEL_FAILED";
  public static final String TRANSACTION_NOT_FOUND = "TRANSACTION_NOT_FOUND";
  public static final String INVALID_REQUEST = "INVALID_REQUEST";
  private PaymentConstants() {}
}
