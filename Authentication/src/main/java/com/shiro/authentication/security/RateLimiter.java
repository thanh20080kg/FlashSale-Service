package com.shiro.authentication.security;

import com.shiro.authentication.config.AppProperties;
import com.shiro.authentication.exception.ApiException;
import com.shiro.authentication.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RateLimiter {
  private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);
  private static final String PREFIX = "rl:";

  private final StringRedisTemplate redis;
  private final AppProperties properties;

  private static String hash(String subject) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(subject.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest, 0, 16);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  public void consume(String ruleName, String subject) {
    if (!properties.getRateLimit().isEnabled()) {
      return;
    }
    AppProperties.Rule rule = properties.getRateLimit().rule(ruleName);
    if (ObjectUtils.isEmpty(rule)) {
      return;
    }

    String key = PREFIX + ruleName + ":" + hash(subject);
    try {
      Long count = redis.opsForValue().increment(key);
      if (ObjectUtils.isEmpty(count)) {
        return;
      }

      if (count == 1L) {
        redis.expire(key, rule.getWindow());
      }

      if (count > rule.getLimit()) {
        log.warn("Rate limit '{}' exceeded ({} > {})", ruleName, count, rule.getLimit());
        throw ApiException.of(ErrorCode.RATE_LIMITED);
      }
    } catch (ApiException rethrow) {
      throw rethrow;
    } catch (RuntimeException redisFailure) {
      // Fail open: a Redis blip must not take authentication down with it.
      log.error("Rate limiter unavailable for rule {}, allowing request", ruleName, redisFailure);
    }
  }
}
