package com.shiro.notification.exception;

import org.springframework.http.HttpStatus;

/** Business failure that is safe to surface to the caller, carrying its own HTTP status. */
public class ApiException extends RuntimeException {
  private final ErrorCode error;

  public ApiException(ErrorCode error) {
    this(error, error.defaultMessage());
  }

  public ApiException(ErrorCode error, String message) {
    super(message);
    this.error = error;
  }

  public static ApiException of(ErrorCode error) {
    return new ApiException(error);
  }

  public ErrorCode error() {
    return error;
  }

  public String getCode() {
    return error.name();
  }

  public HttpStatus getStatus() {
    return error.status();
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    // Expected control flow (sold out, daily limit, bad OTP) fires on every hot request;
    // skipping stack capture keeps it cheap under load.
    return this;
  }
}
