package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.repository.OtpChallengeRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps spent OTP rows from accumulating. Safe to run on every instance; the delete is idempotent.
 */
@Component
@ConditionalOnProperty(name = "app.inventory-sync.enabled", havingValue = "true")
public class ExpiredOtpCleanupWorker {
  private static final Logger log = LoggerFactory.getLogger(ExpiredOtpCleanupWorker.class);
  private static final Duration RETENTION = Duration.ofDays(1);

  private final OtpChallengeRepository otps;

  public ExpiredOtpCleanupWorker(OtpChallengeRepository otps) {
    this.otps = otps;
  }

  @Scheduled(fixedDelay = 3_600_000, initialDelay = 600_000)
  @Transactional
  public void purge() {
    int deleted = otps.deleteExpiredBefore(Instant.now().minus(RETENTION));
    if (deleted > 0) log.info("Purged {} expired OTP challenge(s)", deleted);
  }
}
