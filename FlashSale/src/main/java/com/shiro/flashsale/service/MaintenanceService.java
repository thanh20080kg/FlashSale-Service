package com.shiro.flashsale.service;

public interface MaintenanceService {
  void reload();

  boolean isMaintenance(String configKey);
}
