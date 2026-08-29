package com.shiro.flashsale.security;

import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;

/** Everything a request needs about its caller, resolved from one token decode. */
public record AuthenticatedPrincipal(
    UUID userId, String tokenId, Collection<? extends GrantedAuthority> authorities) {

  @Override
  public String toString() {
    return userId.toString();
  }
}
