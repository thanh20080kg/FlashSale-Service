package com.shiro.flashsale.service;

public interface MaintenanceConfigService {
  void reload();

  boolean isMaintenance(String configKey);
}
