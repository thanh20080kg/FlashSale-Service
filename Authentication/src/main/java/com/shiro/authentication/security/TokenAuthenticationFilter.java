package com.shiro.authentication.security;

import com.shiro.authentication.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates bearer tokens only while their jti is active in shared Redis. */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
  private static final String BEARER = "Bearer ";
  private final AuthService authService;

  public TokenAuthenticationFilter(AuthService authService) {
    this.authService = authService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (ObjectUtils.isNotEmpty(header) && header.startsWith(BEARER)) {
      authService
          .authenticate(header.substring(BEARER.length()).trim())
          .ifPresent(
              principal ->
                  SecurityContextHolder.getContext()
                      .setAuthentication(
                          UsernamePasswordAuthenticationToken.authenticated(
                              principal, null, principal.authorities())));
    }
    chain.doFilter(request, response);
  }
}
