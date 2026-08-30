package com.shiro.flashsale.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
  @Bean
  Clock clock(AppProperties properties) {
    return Clock.system(properties.getTimezone());
  }
}
