package com.shiro.flashsale.config;

import java.time.Duration;
import java.time.ZoneId;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Every tunable knob of the service lives here so nothing is hard-coded in business code. */
@ConfigurationProperties(prefix = "app")
@Component
@Getter
public class AppProperties {
  private final Auth auth = new Auth();
  private final Sale sale = new Sale();
  private final InventorySync inventorySync = new InventorySync();
  private ZoneId timezone = ZoneId.of("Asia/Ho_Chi_Minh");

  @Setter
  @Getter
  public static class Auth {
    private Duration tokenTtl = Duration.ofHours(24);
    private Duration otpTtl = Duration.ofMinutes(5);
    private Duration registrationTtl = Duration.ofHours(1);
    private int otpMaxAttempts = 5;
  }

  @Setter
  @Getter
  public static class Sale {
    private Duration currentItemsCacheTtl = Duration.ofSeconds(2);
  }

  @Setter
  @Getter
  public static class InventorySync {
    private boolean enabled = true;
    private Duration interval = Duration.ofSeconds(1);
    private int batchSize = 200;
    private int maxAttempts = 10;
    private Duration retryBackoff = Duration.ofSeconds(5);
  }
}
