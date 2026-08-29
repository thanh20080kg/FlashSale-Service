package com.shiro.flashsale.security;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed-window counter backed by Redis.
 *
 * <p>Redis rather than an in-process cache on purpose: the limit has to hold across the whole
 * fleet, otherwise adding instances would silently multiply what an attacker is allowed to do.
 */
@Component
public class RateLimiter {
  private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);
  private static final String PREFIX = "rl:";

  private final StringRedisTemplate redis;
  private final AppProperties properties;

  public RateLimiter(StringRedisTemplate redis, AppProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  /** Subjects can be emails or phone numbers - never store them in Redis in the clear. */
  private static String hash(String subject) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(subject.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest, 0, 16);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  /**
   * Consumes one token of {@code ruleName} for {@code subject}, throwing when the rule is spent.
   */
  public void consume(String ruleName, String subject) {
    if (!properties.getRateLimit().isEnabled()) return;
    AppProperties.Rule rule = properties.getRateLimit().rule(ruleName);
    if (rule == null) return;

    String key = PREFIX + ruleName + ":" + hash(subject);
    try {
      Long count = redis.opsForValue().increment(key);
      if (count == null) return;
      if (count == 1L) redis.expire(key, rule.getWindow());
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

  public Duration windowOf(String ruleName) {
    AppProperties.Rule rule = properties.getRateLimit().rule(ruleName);
    return rule == null ? Duration.ZERO : rule.getWindow();
  }
}
