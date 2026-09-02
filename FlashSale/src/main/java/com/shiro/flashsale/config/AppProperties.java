package com.shiro.flashsale.config;

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
  private final Auth auth = new Auth();
  private final Sale sale = new Sale();
  private final Warehouse warehouse = new Warehouse();
  private final Payment payment = new Payment();
  private final KafkaTopic kafkaTopic = new KafkaTopic();
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
    private int limitDailyPurchase;
    private Duration currentItemsCacheTtl = Duration.ofSeconds(2);
  }

  @Setter
  @Getter
  public static class Warehouse {
    private String queue;
    private String statusSyncQueue;
    private Duration timeout = Duration.ofSeconds(15);
  }

  @Setter
  @Getter
  public static class Payment {
    private String queue;
    private Duration timeout;
    private int syncBatchSize;
  }

  @Setter
  @Getter
  public static class KafkaTopic {
    private String triggerReload;
    private String quotaReloadTopic;
    private String paymentStatusSync;
  }
}
