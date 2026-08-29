package com.shiro.flashsale.controller;

import com.shiro.flashsale.dto.AuthDtos;
import com.shiro.flashsale.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/register")
  public AuthDtos.MessageResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
    service.register(request);
    return new AuthDtos.MessageResponse("Registration created. OTP sent.");
  }

  @PostMapping("/verify-otp")
  public AuthDtos.MessageResponse verify(@Valid @RequestBody AuthDtos.VerifyOtpRequest request) {
    service.verifyOtp(request);
    return new AuthDtos.MessageResponse("Identifier verified successfully");
  }

  @PostMapping("/login")
  public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
    return service.login(request);
  }

  @PostMapping("/logout")
  public AuthDtos.MessageResponse logout(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    if (authorization != null && authorization.startsWith("Bearer "))
      service.logout(authorization.substring(7).trim());
    return new AuthDtos.MessageResponse("Logged out");
  }
}
