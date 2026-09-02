package com.shiro.flashsale.security;

import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import java.util.UUID;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.Authentication;

public final class CurrentUser {
  private CurrentUser() {}

  public static UUID id(Authentication authentication) {
    if (ObjectUtils.isEmpty(authentication)
        || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal p)) {
      throw ApiException.of(ErrorCode.UNAUTHENTICATED);
    }
    return p.userId();
  }
}
