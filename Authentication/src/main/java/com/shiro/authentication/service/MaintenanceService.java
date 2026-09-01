package com.shiro.authentication.service;

public interface MaintenanceService {
  void reload();

  boolean isMaintenance(String configKey);
}
