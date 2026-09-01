package com.shiro.flashsale.security;

import com.shiro.flashsale.constants.RedisKeyConstants;
import com.shiro.flashsale.service.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
  private static final String BEARER = "Bearer ";

  private final JwtDecoder jwtDecoder;
  private final RedisService redisService;

  public TokenAuthenticationFilter(JwtDecoder jwtDecoder, RedisService redisService) {
    this.jwtDecoder = jwtDecoder;
    this.redisService = redisService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (ObjectUtils.isNotEmpty(header) && header.startsWith(BEARER)) {
      try {
        Jwt jwt = jwtDecoder.decode(header.substring(BEARER.length()).trim());
        String tokenId = jwt.getId();
        String userId =
            ObjectUtils.isEmpty(tokenId)
                ? null
                : redisService.get(RedisKeyConstants.AUTH_TOKEN + tokenId);
        if (ObjectUtils.isNotEmpty(userId)) {
          List<GrantedAuthority> authorities = new ArrayList<>();
          String role = jwt.getClaimAsString("role");
          if (ObjectUtils.isNotEmpty(role)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
          }
          List<String> permissions = jwt.getClaimAsStringList("permissions");
          if (ObjectUtils.isNotEmpty(permissions)) {
            permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority("PERM_" + p)));
          }
          AuthenticatedPrincipal principal =
              new AuthenticatedPrincipal(java.util.UUID.fromString(userId), tokenId, authorities);
          SecurityContextHolder.getContext()
              .setAuthentication(
                  UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
        }
      } catch (RuntimeException ignored) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
