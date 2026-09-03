package com.shiro.authentication.constants;

public final class RedisKeyConstants {
  public static final String SERVICE = "authenticate:";
  public static final String COMMON = "common:";
  public static final String AUTH_TOKEN = COMMON + "auth:token:";
  public static final String APP_CONFIG = SERVICE + "app:config:";
  public static final String RATE_LIMIT_PREFIX = SERVICE + "rl:";

  private RedisKeyConstants() {}
}
