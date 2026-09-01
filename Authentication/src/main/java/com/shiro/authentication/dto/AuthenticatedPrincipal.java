package com.shiro.authentication.dto;

import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;

public record AuthenticatedPrincipal(
    UUID userId, String tokenId, Collection<? extends GrantedAuthority> authorities) {

  @Override
  public String toString() {
    return userId.toString();
  }
}
