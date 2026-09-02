package com.shiro.authentication.service.impl;

import com.shiro.authentication.entity.AppConfiguration;
import com.shiro.authentication.repository.AppConfigurationRepository;
import com.shiro.authentication.service.MaintenanceService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class MaintenanceServiceImpl implements MaintenanceService {
  private static final String ALL_APIS_KEY = "MAINTENANCE_ALL";
  private final AppConfigurationRepository configurationsRepository;
  private volatile Map<String, String> configurations = Map.of();

  public MaintenanceServiceImpl(AppConfigurationRepository configurationsRepository) {
    this.configurationsRepository = configurationsRepository;
  }

  private static boolean isOn(String value) {
    return Boolean.parseBoolean(value);
  }

  @Override
  @EventListener(ApplicationReadyEvent.class)
  public void reload() {
    Map<String, String> loaded = new HashMap<>();
    List<AppConfiguration> appConfigurations =
        configurationsRepository.findAll().stream()
            .filter(appConfiguration -> appConfiguration.getKey().startsWith("MAINTENANCE"))
            .toList();
    for (AppConfiguration configuration : appConfigurations) {
      loaded.put(configuration.getKey(), configuration.getValue());
    }
    configurations = Map.copyOf(loaded);
  }

  @Override
  public boolean isMaintenance(String configKey) {
    if (isOn(configurations.get(ALL_APIS_KEY))) {
      return true;
    }
    return ObjectUtils.isNotEmpty(configKey) && isOn(configurations.get(configKey));
  }
}
