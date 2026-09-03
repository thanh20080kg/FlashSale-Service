package com.shiro.flashsale.constants;

public final class RedisKeyConstants {
  public static final String SERVICE = "flashsale:";
  public static final String COMMON = "common:";
  public static final String SALE_CURRENT = SERVICE + "sale:current:";
  public static final String ITEM_QUOTA = SERVICE + "item:quota:";
  public static final String AUTH_TOKEN = COMMON + "auth:token:";
  public static final String APP_CONFIG = SERVICE + "app:config:";

  private RedisKeyConstants() {}
}
