package com.shiro.authentication.exception;

/** Signals an invalid OTP while allowing the failed-attempt counter to commit. */
public class OtpInvalidException extends AuthException {
  public OtpInvalidException() {
    super(ErrorCode.OTP_INVALID);
  }
}
