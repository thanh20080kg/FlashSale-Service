package com.shiro.flashsale.exception;

/** Kept as a narrower alias of {@link ApiException} for authentication flows. */
public class AuthException extends ApiException {
  public AuthException(ErrorCode error) {
    super(error);
  }

  public AuthException(ErrorCode error, String message) {
    super(error, message);
  }
}
