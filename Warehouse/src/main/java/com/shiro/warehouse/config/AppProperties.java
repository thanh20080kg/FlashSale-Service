package com.shiro.warehouse.config;

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

  @Setter
  @Getter
  public static class RabbitQueue {
    private String inventoryQueue;
    private String statusSyncQueue;
  }
}
