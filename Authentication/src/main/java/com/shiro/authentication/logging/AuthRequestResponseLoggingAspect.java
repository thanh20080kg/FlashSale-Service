package com.shiro.authentication.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.apache.commons.lang3.ObjectUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Logs API boundaries and the application functions reached by each request. */
@Aspect
@Component
@Order(1)
public class AuthRequestResponseLoggingAspect {
  private static final Logger log = LoggerFactory.getLogger(AuthRequestResponseLoggingAspect.class);
  private final ObjectMapper objectMapper;

  public AuthRequestResponseLoggingAspect(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Around("within(com.shiro.authentication.controller..*)")
  public Object logRequestResponse(ProceedingJoinPoint joinPoint) throws Throwable {
    RequestContext context = beginTrace();
    String operation = operation(joinPoint);
    log.info(
        "HTTP_REQUEST requestId={} threadId={} operation={} body={}",
        context.requestId(),
        context.threadId(),
        operation,
        json(joinPoint.getArgs()));
    try {
      Object response = joinPoint.proceed();
      log.info(
          "HTTP_RESPONSE requestId={} threadId={} operation={} body={}",
          context.requestId(),
          context.threadId(),
          operation,
          json(response));
      return response;
    } finally {
      endTrace(context);
    }
  }

  @Around(
      "execution(public * com.shiro.authentication..*(..))"
          + " && !within(com.shiro.authentication.config..*)"
          + " && !within(com.shiro.authentication.logging..*)"
          + " && !within(com.shiro.authentication.controller..*)")
  public Object logFunctionTrace(ProceedingJoinPoint joinPoint) throws Throwable {
    RequestContext context = beginTrace();
    log.debug("FUNCTION_TRACE operation={}", operation(joinPoint));
    try {
      return joinPoint.proceed();
    } finally {
      endTrace(context);
    }
  }

  private RequestContext beginTrace() {
    String previous = MDC.get("requestId");
    String requestId = ObjectUtils.isEmpty(previous) ? UUID.randomUUID().toString() : previous;
    if (ObjectUtils.isEmpty(previous)) {
      MDC.put("requestId", requestId);
    }
    return new RequestContext(
        requestId, Thread.currentThread().getId(), ObjectUtils.isEmpty(previous));
  }

  private void endTrace(RequestContext context) {
    if (context.root()) {
      MDC.remove("requestId");
    }
  }

  private String operation(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception exception) {
      return "[unserializable]";
    }
  }

  private record RequestContext(String requestId, long threadId, boolean root) {}
}
