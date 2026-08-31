package com.shiro.authentication.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app")
@Component
@Getter
@Setter
public class AppProperties {
  private final Auth auth = new Auth();
  private final RateLimit rateLimit = new RateLimit();
  private ZoneId timezone = ZoneId.of("Asia/Ho_Chi_Minh");

  @Setter
  @Getter
  public static class Auth {
    private Duration tokenTtl = Duration.ofHours(24);
    private Duration otpTtl = Duration.ofMinutes(5);
    private Duration registrationTtl = Duration.ofMinutes(15);
    private int otpMaxAttempts = 5;
  }

  @Setter
  @Getter
  public static class RateLimit {
    private boolean enabled = true;
    private Map<String, Rule> rules = new LinkedHashMap<>();

    public Rule rule(String name) {
      return rules.get(name);
    }
  }

  @Setter
  @Getter
  public static class Rule {
    private long limit = 60;
    private Duration window = Duration.ofMinutes(1);
  }
}
