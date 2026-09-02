package com.shiro.notification.logging;

import java.util.UUID;
import lombok.AllArgsConstructor;
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
import tools.jackson.databind.ObjectMapper;

@Aspect
@Component
@Order(1)
@AllArgsConstructor
public class LoggingAspect {
  private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
  private final ObjectMapper objectMapper;

  @Around("execution(public * com.shiro.notification..*(..))")
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
