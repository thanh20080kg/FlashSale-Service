package com.shiro.flashsale.config;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfiguration {
  private static final Logger log = LoggerFactory.getLogger(ApplicationConfiguration.class);

  private final AppProperties properties;
  private final String jwtSecret;

  public ApplicationConfiguration(
      AppProperties properties,
      @Value("${spring.security.oauth2.resourceserver.jwt.secret-key}") String jwtSecret) {
    this.properties = properties;
    this.jwtSecret = jwtSecret;
  }

  /**
   * A single injectable clock pinned to the configured business timezone. Business code never calls
   * {@code LocalDate.now()} directly, so every instance agrees on "today" and tests can freeze time.
   */
  @Bean
  public Clock clock() {
    return Clock.system(properties.getTimezone());
  }

  @EventListener(ApplicationReadyEvent.class)
  void warnOnInsecureSecret() {
    if (!properties.getInsecureDefaultJwtSecret().isBlank()
        && properties.getInsecureDefaultJwtSecret().equals(jwtSecret)) {
      log.warn(
          "JWT_SECRET is not set - falling back to the well-known local development secret. "
              + "Set the JWT_SECRET environment variable before deploying anywhere real.");
    }
  }
}
