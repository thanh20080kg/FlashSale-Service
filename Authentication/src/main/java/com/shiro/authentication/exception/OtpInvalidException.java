package com.shiro.authentication.exception;

public class OtpInvalidException extends AuthException {
  public OtpInvalidException() {
    super(ErrorCode.OTP_INVALID);
  }
}
