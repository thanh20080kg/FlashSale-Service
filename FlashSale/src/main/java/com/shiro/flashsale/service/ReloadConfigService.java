package com.shiro.flashsale.service;

public interface ReloadConfigService {
  void reload();

  boolean isMaintenance(String configKey);

  int getLimitDailyPurchase();
}
