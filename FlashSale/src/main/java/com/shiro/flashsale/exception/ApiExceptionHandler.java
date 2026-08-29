package com.shiro.flashsale.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, Object>> api(ApiException ex, HttpServletRequest req) {
    return response(ex.error(), ex.getMessage(), req);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> validation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(e -> e.getField() + " " + e.getDefaultMessage())
            .orElse(ErrorCode.VALIDATION_ERROR.defaultMessage());
    return response(ErrorCode.VALIDATION_ERROR, message, req);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<Map<String, Object>> malformed(Exception ex, HttpServletRequest req) {
    return response(ErrorCode.VALIDATION_ERROR, "Malformed request payload", req);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, Object>> unauthenticated(
      AuthenticationException ex, HttpServletRequest req) {
    return response(ErrorCode.UNAUTHENTICATED, ErrorCode.UNAUTHENTICATED.defaultMessage(), req);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> forbidden(
      AccessDeniedException ex, HttpServletRequest req) {
    return response(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage(), req);
  }

  /**
   * A unique-key violation reaching this point means two concurrent requests raced on the same
   * natural key. It is a conflict, not a server fault, and the detail never leaves the log.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> conflict(
      DataIntegrityViolationException ex, HttpServletRequest req) {
    log.warn(
        "Data integrity violation on {}: {}",
        req.getRequestURI(),
        ex.getMostSpecificCause().getMessage());
    return response(
        ErrorCode.DUPLICATE_RESOURCE, ErrorCode.DUPLICATE_RESOURCE.defaultMessage(), req);
  }

  @ExceptionHandler({HttpRequestMethodNotSupportedException.class, NoHandlerFoundException.class})
  public ResponseEntity<Map<String, Object>> notFound(Exception ex, HttpServletRequest req) {
    return response(ErrorCode.INVALID_REQUEST, "No handler for this request", req);
  }

  /** Last resort: log the cause, return an opaque body so internals never leak. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> unexpected(Exception ex, HttpServletRequest req) {
    log.error("Unhandled error on {} {}", req.getMethod(), req.getRequestURI(), ex);
    return response(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), req);
  }

  private ResponseEntity<Map<String, Object>> response(
      ErrorCode error, String message, HttpServletRequest req) {
    HttpStatus status = error.status();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now());
    body.put("status", status.value());
    body.put("code", error.name());
    body.put("message", message);
    body.put("path", req.getRequestURI());
    return ResponseEntity.status(status).body(body);
  }
}
