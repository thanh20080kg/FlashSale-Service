package com.shiro.flashsale.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.shiro.flashsale.service.CacheConfigService;
import com.shiro.flashsale.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfiguration {

  private final CacheConfigService cacheConfigService;
  private final PurchaseService purchaseService;

  @Bean
  ObjectMapper objectMapper() {
    return JsonMapper.builder().findAndAddModules().build();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void startUpRuntime() {
    cacheConfigService.reload();
    purchaseService.reloadQuota();
  }
}
