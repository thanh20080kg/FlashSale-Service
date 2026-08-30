package com.shiro.authentication.security;

import com.shiro.authentication.exception.ApiException;
import com.shiro.authentication.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ObjectUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Per-IP throttling of the endpoints worth abusing. Identifier-scoped limits (per email/phone) are
 * applied deeper in {@code AuthServiceImpl}, where the identifier is known and normalised.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

  private static final List<Route> ROUTES =
      List.of(
          new Route("POST", "/api/v1/auth/login", "auth-login-ip"),
          new Route("POST", "/api/v1/auth/register", "auth-register-ip"),
          new Route("POST", "/api/v1/auth/verify-otp", "auth-otp-ip"));
  private final RateLimiter rateLimiter;
  private final ObjectMapper objectMapper;

  public RateLimitFilter(RateLimiter rateLimiter, ObjectMapper objectMapper) {
    this.rateLimiter = rateLimiter;
    this.objectMapper = objectMapper;
  }

  private static String ruleFor(HttpServletRequest request) {
    String path = request.getRequestURI();
    return ROUTES.stream()
        .filter(
            route ->
                ObjectUtils.equals(route.method(), request.getMethod())
                    && ObjectUtils.equals(route.path(), path))
        .findFirst()
        .map(Route::rule)
        .orElseGet(() -> null);
  }

  /**
   * Behind a load balancer the socket address is the proxy, so the first hop of X-Forwarded-For is
   * used when present. Trust it only because the service is expected to sit behind an ingress that
   * rewrites the header.
   */
  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (ObjectUtils.isNotEmpty(forwarded) && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
    }
    return request.getRemoteAddr();
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    String rule = ruleFor(request);
    if (ObjectUtils.isNotEmpty(rule)) {
      try {
        rateLimiter.consume(rule, clientIp(request));
      } catch (ApiException ex) {
        writeError(request, response, ex);
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private void writeError(HttpServletRequest request, HttpServletResponse response, ApiException ex)
      throws IOException {
    response.setStatus(ex.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", ex.getStatus().value());
    body.put("code", ErrorCode.RATE_LIMITED.name());
    body.put("message", ex.getMessage());
    body.put("path", request.getRequestURI());
    objectMapper.writeValue(response.getOutputStream(), body);
  }

  private record Route(String method, String path, String rule) {}
}
