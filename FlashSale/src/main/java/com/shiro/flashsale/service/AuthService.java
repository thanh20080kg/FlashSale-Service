package com.shiro.flashsale.service;

import com.shiro.flashsale.dto.AuthDtos;
import com.shiro.flashsale.security.AuthenticatedPrincipal;
import java.util.Optional;

public interface AuthService {
  void register(AuthDtos.RegisterRequest request);

  void verifyOtp(AuthDtos.VerifyOtpRequest request);

  AuthDtos.AuthResponse login(AuthDtos.LoginRequest request);

  void logout(String token);

  /** Resolves a bearer token to its caller in a single decode plus a single session lookup. */
  Optional<AuthenticatedPrincipal> authenticate(String token);
}
