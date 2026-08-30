package com.shiro.authentication.controller;

import com.shiro.authentication.dto.AuthResponse;
import com.shiro.authentication.dto.LoginRequest;
import com.shiro.authentication.dto.MessageResponse;
import com.shiro.authentication.dto.RegisterRequest;
import com.shiro.authentication.dto.VerifyOtpRequest;
import com.shiro.authentication.service.AuthService;
import jakarta.validation.Valid;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/register")
  public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
    service.register(request);
    return new MessageResponse("Registration created. OTP sent.");
  }

  @PostMapping("/verify-otp")
  public AuthResponse verify(@Valid @RequestBody VerifyOtpRequest request) {
    return service.verifyOtp(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return service.login(request);
  }

  @PostMapping("/logout")
  public MessageResponse logout(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    if (ObjectUtils.isNotEmpty(authorization) && authorization.startsWith("Bearer ")) {
      service.logout(authorization.substring(7).trim());
    }
    return new MessageResponse("Logged out");
  }
}
