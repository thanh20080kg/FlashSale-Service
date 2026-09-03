package com.shiro.authentication.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Provides the Redis primitives used by authentication flows. */
@Service
@RequiredArgsConstructor
public class RedisService {
  private final StringRedisTemplate redisTemplate;

  public String get(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  public void set(String key, String value, Duration ttl) {
    redisTemplate.opsForValue().set(key, value, ttl);
  }

  public void set(String key, String value) {
    redisTemplate.opsForValue().set(key, value);
  }
}
