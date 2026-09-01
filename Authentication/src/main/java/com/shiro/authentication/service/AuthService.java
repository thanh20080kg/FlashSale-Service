package com.shiro.authentication.service;

import com.shiro.authentication.dto.AuthResponse;
import com.shiro.authentication.dto.AuthenticatedPrincipal;
import com.shiro.authentication.dto.LoginRequest;
import com.shiro.authentication.dto.RegisterRequest;
import com.shiro.authentication.dto.VerifyOtpRequest;
import java.util.Optional;

public interface AuthService {
  void register(RegisterRequest request);

  AuthResponse verifyOtp(VerifyOtpRequest request);

  AuthResponse login(LoginRequest request);

  void logout(String token);

  Optional<AuthenticatedPrincipal> authenticate(String token);
}
