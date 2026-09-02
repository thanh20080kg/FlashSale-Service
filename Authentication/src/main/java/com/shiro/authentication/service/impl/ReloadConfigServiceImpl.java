package com.shiro.authentication.service.impl;

import com.shiro.authentication.constants.RedisKeyConstants;
import com.shiro.authentication.entity.AppConfiguration;
import com.shiro.authentication.repository.AppConfigurationRepository;
import com.shiro.authentication.service.RedisService;
import com.shiro.authentication.service.ReloadConfigService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReloadConfigServiceImpl implements ReloadConfigService {
  private static final String ALL_APIS_KEY = "MAINTENANCE_ALL";
  private static final String DAILY_PURCHASE_LIMIT_KEY = "SALE_LIMIT_DAILY_PURCHASE";

  private final AppConfigurationRepository configurationsRepository;
  private final RedisService redisService;
  private volatile Map<String, String> fallbackConfigurations = Map.of();

  private static boolean isOn(String value) {
    return Boolean.parseBoolean(value);
  }

  @Override
  @EventListener(ApplicationReadyEvent.class)
  public void reload() {
    Map<String, String> loaded = new HashMap<>();
    List<AppConfiguration> appConfigurations = configurationsRepository.findAll();
    for (AppConfiguration configuration : appConfigurations) {
      loaded.put(configuration.getKey(), configuration.getValue());
      redisService.set(
          RedisKeyConstants.APP_CONFIG + configuration.getKey(), configuration.getValue());
    }
    fallbackConfigurations = Map.copyOf(loaded);
  }

  @Override
  public boolean isMaintenance(String configKey) {
    if (isOn(getConfiguration(ALL_APIS_KEY))) {
      return true;
    }
    return ObjectUtils.isNotEmpty(configKey) && isOn(getConfiguration(configKey));
  }

  private String getConfiguration(String key) {
    String cached = redisService.get(RedisKeyConstants.APP_CONFIG + key);
    return cached != null ? cached : fallbackConfigurations.get(key);
  }
}
