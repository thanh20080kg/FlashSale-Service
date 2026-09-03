package com.shiro.flashsale.service;

public interface CacheConfigService {
  void reload();

  boolean isMaintenance(String configKey);

  int getLimitDailyPurchase();
}
