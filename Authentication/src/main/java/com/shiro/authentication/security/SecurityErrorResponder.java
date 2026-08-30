package com.shiro.authentication.security;

import com.shiro.authentication.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Denials happen inside the filter chain, before any controller advice can see them, so 401 and 403
 * have to be rendered here to keep every error response in the same shape.
 */
@Component
public class SecurityErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {
  private final ObjectMapper objectMapper;

  public SecurityErrorResponder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull AuthenticationException ex)
      throws IOException {
    write(request, response, ErrorCode.UNAUTHENTICATED);
  }

  @Override
  public void handle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull AccessDeniedException ex)
      throws IOException {
    write(request, response, ErrorCode.FORBIDDEN);
  }

  private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode error)
      throws IOException {
    response.setStatus(error.status().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", error.status().value());
    body.put("code", error.name());
    body.put("message", error.defaultMessage());
    body.put("path", request.getRequestURI());
    objectMapper.writeValue(response.getOutputStream(), body);
  }
}
