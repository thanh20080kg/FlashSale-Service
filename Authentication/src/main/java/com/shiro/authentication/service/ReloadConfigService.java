package com.shiro.authentication.service;

public interface ReloadConfigService {
  void reload();

  boolean isMaintenance(String configKey);
}
