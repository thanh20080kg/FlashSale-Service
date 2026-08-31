package com.shiro.authentication.service;

public interface MaintenanceConfigService {
  void reload();

  boolean isMaintenance(String configKey);
}
