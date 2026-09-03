package com.shiro.warehouse.config;

import java.time.Duration;
import java.time.ZoneId;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  private ZoneId timezone = ZoneId.of("Asia/Ho_Chi_Minh");
  private RabbitQueue rabbitQueue;
  private StatusSync statusSync = new StatusSync();

  @Setter
  @Getter
  public static class RabbitQueue {
    private String inventoryQueue;
    private String statusSyncQueue;
  }

  @Setter
  @Getter
  public static class StatusSync {
    private int batchSize = 100;
    private Duration syncAge = Duration.ofMinutes(5);
  }
}
