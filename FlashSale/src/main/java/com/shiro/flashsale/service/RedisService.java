package com.shiro.flashsale.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Provides the Redis operations used by the flash-sale workflow. */
@Service
@RequiredArgsConstructor
public class RedisService {
  private final StringRedisTemplate redisTemplate;

  /** Reads a string value by key. */
  public String get(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  /** Stores a value with a time-to-live. */
  public void set(String key, String value, Duration ttl) {
    redisTemplate.opsForValue().set(key, value, ttl);
  }

  /** Stores a value only when the key does not already exist. */
  public Boolean setIfAbsent(String key, String value, Duration ttl) {
    return redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
  }

  /** Stores a value without setting an expiration. */
  public void set(String key, String value) {
    redisTemplate.opsForValue().set(key, value);
  }

  /** Atomically increments a numeric value. */
  public Long increment(String key) {
    return redisTemplate.opsForValue().increment(key);
  }

  /** Atomically increments a numeric value. */
  public Long increment(String key, int i) {
    return redisTemplate.opsForValue().increment(key, i);
  }

  /** Atomically decrements a numeric value. */
  public Long decrement(String key) {
    return redisTemplate.opsForValue().decrement(key);
  }

  /** Atomically decrements a numeric value. */
  public Long decrement(String key, int i) {
    return redisTemplate.opsForValue().decrement(key, i);
  }

  /** Applies an expiration duration to an existing key. */
  public Boolean expire(String key, Duration ttl) {
    return redisTemplate.expire(key, ttl);
  }
}
