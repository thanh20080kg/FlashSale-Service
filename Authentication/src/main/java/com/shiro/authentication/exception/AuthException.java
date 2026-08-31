package com.shiro.authentication.exception;

public class AuthException extends ApiException {
  public AuthException(ErrorCode error) {
    super(error);
  }

  public AuthException(ErrorCode error, String message) {
    super(error, message);
  }
}
