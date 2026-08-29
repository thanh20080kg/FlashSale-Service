package com.shiro.flashsale.dto;

import jakarta.validation.constraints.*;

public final class AuthDtos {
  private AuthDtos() {}

  /**
   * {@code identifier} carries either an email or a phone number; the service decides which by
   * matching it, so registration stays a single endpoint as required.
   */
  public record RegisterRequest(
      @NotBlank @Size(max = 190) String identifier,
      @NotBlank @Size(min = 8, max = 72) String password,
      @Size(max = 100) String displayName) {}

  public record LoginRequest(
      @NotBlank @Size(max = 190) String identifier, @NotBlank @Size(max = 72) String password) {}

  public record VerifyOtpRequest(
      @NotBlank @Size(max = 190) String identifier,
      @NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String otp) {}

  public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {}

  public record MessageResponse(String message) {}
}
