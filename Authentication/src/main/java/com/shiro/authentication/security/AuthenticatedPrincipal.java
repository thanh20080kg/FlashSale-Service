package com.shiro.authentication.security;

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
