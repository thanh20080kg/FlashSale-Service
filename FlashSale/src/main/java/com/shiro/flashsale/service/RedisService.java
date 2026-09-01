package com.shiro.flashsale.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

  public Long increment(String key) {
    return redisTemplate.opsForValue().increment(key);
  }

  public Long decrement(String key) {
    return redisTemplate.opsForValue().decrement(key);
  }

  public Boolean expire(String key, Duration ttl) {
    return redisTemplate.expire(key, ttl);
  }
}
