package com.shiro.flashsale.security;

import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import java.util.UUID;
import org.springframework.security.core.Authentication;

/** Extracts the caller's user id without controllers having to know how the principal is shaped. */
public final class CurrentUser {
  private CurrentUser() {}

  public static UUID id(Authentication authentication) {
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal p))
      throw ApiException.of(ErrorCode.UNAUTHENTICATED);
    return p.userId();
  }
}
