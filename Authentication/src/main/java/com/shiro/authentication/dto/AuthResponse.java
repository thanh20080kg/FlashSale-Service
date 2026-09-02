package com.shiro.authentication.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthResponse {
  private final String accessToken;
  private final String tokenType;
  private final long expiresInSeconds;
}
