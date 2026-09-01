package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.entity.AppConfiguration;
import com.shiro.flashsale.repository.AppConfigurationRepository;
import com.shiro.flashsale.service.MaintenanceService;
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
  private volatile Map<String, String> configurations = Map.of();

  private final AppConfigurationRepository configurationsRepository;

  public MaintenanceServiceImpl(AppConfigurationRepository configurationsRepository) {
    this.configurationsRepository = configurationsRepository;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Override
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

  private static boolean isOn(String value) {
    return Boolean.parseBoolean(value);
  }
}
