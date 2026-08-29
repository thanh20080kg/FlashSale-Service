package com.shiro.flashsale.security;

import com.shiro.flashsale.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
  private static final String BEARER = "Bearer ";

  private final AuthService auth;

  public TokenAuthenticationFilter(AuthService auth) {
    this.auth = auth;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER)) {
      // One decode, one Redis lookup per request - this runs on every call, including the
      // purchase hot path.
      Optional<AuthenticatedPrincipal> principal =
          auth.authenticate(header.substring(BEARER.length()).trim());
      principal.ifPresent(
          p ->
              SecurityContextHolder.getContext()
                  .setAuthentication(
                      UsernamePasswordAuthenticationToken.authenticated(p, null, p.authorities())));
    }
    chain.doFilter(request, response);
  }
}
