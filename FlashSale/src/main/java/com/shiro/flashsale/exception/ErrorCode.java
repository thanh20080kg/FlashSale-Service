package com.shiro.flashsale.exception;

import org.springframework.http.HttpStatus;

/**
 * Single catalogue of business error codes and the HTTP status each maps to. Messages stay
 * deliberately generic so no account existence, balance or stock detail leaks to the caller.
 */
public enum ErrorCode {
  // --- auth ---
  INVALID_IDENTIFIER(HttpStatus.BAD_REQUEST, "identifier must be a valid email or phone number"),
  IDENTIFIER_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Email/phone is already registered"),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
  IDENTIFIER_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Verify your email/phone with OTP first"),
  OTP_INVALID(HttpStatus.BAD_REQUEST, "OTP is invalid or expired"),
  OTP_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Too many OTP attempts, request a new code"),
  REGISTRATION_EXPIRED(HttpStatus.GONE, "Registration request has expired"),
  REGISTRATION_INVALID(HttpStatus.BAD_REQUEST, "Registration request is invalid"),
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
  FORBIDDEN(HttpStatus.FORBIDDEN, "You are not allowed to perform this action"),
  RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests, slow down"),

  // --- sale ---
  CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "Customer not found"),
  SALE_NOT_ACTIVE(HttpStatus.CONFLICT, "Flash sale item is not active right now"),
  SOLD_OUT(HttpStatus.CONFLICT, "Flash sale item is sold out"),
  OUT_OF_STOCK(HttpStatus.CONFLICT, "Product inventory is sold out"),
  INSUFFICIENT_BALANCE(HttpStatus.PAYMENT_REQUIRED, "Insufficient balance"),
  DAILY_LIMIT_REACHED(HttpStatus.CONFLICT, "Only one flash sale purchase is allowed per day"),
  PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "Payment failed"),

  // --- admin / catalogue ---
  PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
  SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "Flash sale slot not found"),
  ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Flash sale item not found"),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
  DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "Resource already exists"),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Invalid request"),

  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");

  private final HttpStatus status;
  private final String defaultMessage;

  ErrorCode(HttpStatus status, String defaultMessage) {
    this.status = status;
    this.defaultMessage = defaultMessage;
  }

  public HttpStatus status() {
    return status;
  }

  public String defaultMessage() {
    return defaultMessage;
  }
}
