package com.shiro.flashsale.security;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;

public record AuthenticatedPrincipal(
    UUID userId, String tokenId, List<? extends GrantedAuthority> authorities) {}
