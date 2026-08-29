package com.shiro.flashsale.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Every tunable knob of the service lives here so nothing is hard-coded in business code. */
@ConfigurationProperties(prefix = "app")
@Component
public class AppProperties {
  private final Seed seed = new Seed();
  private final Auth auth = new Auth();
  private final Sale sale = new Sale();
  private final InventorySync inventorySync = new InventorySync();
  private final RateLimit rateLimit = new RateLimit();
  private ZoneId timezone = ZoneId.of("Asia/Ho_Chi_Minh");
  private String insecureDefaultJwtSecret = "";

  public ZoneId getTimezone() {
    return timezone;
  }

  public void setTimezone(ZoneId timezone) {
    this.timezone = timezone;
  }

  public String getInsecureDefaultJwtSecret() {
    return insecureDefaultJwtSecret;
  }

  public void setInsecureDefaultJwtSecret(String insecureDefaultJwtSecret) {
    this.insecureDefaultJwtSecret = insecureDefaultJwtSecret;
  }

  public Seed getSeed() {
    return seed;
  }

  public Auth getAuth() {
    return auth;
  }

  public Sale getSale() {
    return sale;
  }

  public InventorySync getInventorySync() {
    return inventorySync;
  }

  public RateLimit getRateLimit() {
    return rateLimit;
  }

  public static class Seed {
    private boolean demoUsers = false;
    private String demoUserPassword = "Password@123";
    private BigDecimal demoUserBalance = new BigDecimal("5000000");

    public boolean isDemoUsers() {
      return demoUsers;
    }

    public void setDemoUsers(boolean demoUsers) {
      this.demoUsers = demoUsers;
    }

    public String getDemoUserPassword() {
      return demoUserPassword;
    }

    public void setDemoUserPassword(String demoUserPassword) {
      this.demoUserPassword = demoUserPassword;
    }

    public BigDecimal getDemoUserBalance() {
      return demoUserBalance;
    }

    public void setDemoUserBalance(BigDecimal demoUserBalance) {
      this.demoUserBalance = demoUserBalance;
    }
  }

  public static class Auth {
    private Duration tokenTtl = Duration.ofHours(24);
    private Duration otpTtl = Duration.ofMinutes(5);
    private Duration registrationTtl = Duration.ofHours(1);
    private int otpMaxAttempts = 5;

    public Duration getTokenTtl() {
      return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
      this.tokenTtl = tokenTtl;
    }

    public Duration getOtpTtl() {
      return otpTtl;
    }

    public void setOtpTtl(Duration otpTtl) {
      this.otpTtl = otpTtl;
    }

    public Duration getRegistrationTtl() {
      return registrationTtl;
    }

    public void setRegistrationTtl(Duration registrationTtl) {
      this.registrationTtl = registrationTtl;
    }

    public int getOtpMaxAttempts() {
      return otpMaxAttempts;
    }

    public void setOtpMaxAttempts(int otpMaxAttempts) {
      this.otpMaxAttempts = otpMaxAttempts;
    }
  }

  public static class Sale {
    private Duration currentItemsCacheTtl = Duration.ofSeconds(2);

    public Duration getCurrentItemsCacheTtl() {
      return currentItemsCacheTtl;
    }

    public void setCurrentItemsCacheTtl(Duration currentItemsCacheTtl) {
      this.currentItemsCacheTtl = currentItemsCacheTtl;
    }
  }

  public static class InventorySync {
    private boolean enabled = true;
    private Duration interval = Duration.ofSeconds(1);
    private int batchSize = 200;
    private int maxAttempts = 10;
    private Duration retryBackoff = Duration.ofSeconds(5);

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getInterval() {
      return interval;
    }

    public void setInterval(Duration interval) {
      this.interval = interval;
    }

    public int getBatchSize() {
      return batchSize;
    }

    public void setBatchSize(int batchSize) {
      this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public Duration getRetryBackoff() {
      return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
      this.retryBackoff = retryBackoff;
    }
  }

  public static class RateLimit {
    private boolean enabled = true;
    private Map<String, Rule> rules = new LinkedHashMap<>();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Map<String, Rule> getRules() {
      return rules;
    }

    public void setRules(Map<String, Rule> rules) {
      this.rules = rules;
    }

    public Rule rule(String name) {
      return rules.get(name);
    }
  }

  public static class Rule {
    private long limit = 60;
    private Duration window = Duration.ofMinutes(1);

    public long getLimit() {
      return limit;
    }

    public void setLimit(long limit) {
      this.limit = limit;
    }

    public Duration getWindow() {
      return window;
    }

    public void setWindow(Duration window) {
      this.window = window;
    }
  }
}
