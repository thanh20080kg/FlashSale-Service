package com.shiro.authentication.service;

public interface CacheConfigService {
  void reload();

  boolean isMaintenance(String configKey);
}
