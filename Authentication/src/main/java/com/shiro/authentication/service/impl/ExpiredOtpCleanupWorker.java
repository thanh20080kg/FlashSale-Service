package com.shiro.authentication.service.impl;

import com.shiro.authentication.repository.OtpChallengeRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.otp-clear.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ExpiredOtpCleanupWorker {
  private static final Logger log = LoggerFactory.getLogger(ExpiredOtpCleanupWorker.class);
  private final OtpChallengeRepository otps;

  @Value("${app.otp-clear.retention}")
  private Duration RETENTION;

  @Scheduled(
      fixedDelayString = "${app.otp-clear.interval}",
      initialDelayString = "${app.otp-clear.initial-delay}")
  @Transactional
  public void purge() {
    try {
      int deleted = otps.deleteExpiredBefore(Instant.now().minus(RETENTION));
      if (deleted > 0) {
        log.info("Purged {} expired OTP challenge(s)", deleted);
      } else {
        log.debug("Expired OTP cleanup completed; no expired challenges found");
      }
    } catch (RuntimeException exception) {
      log.error("Failed to purge expired OTP challenges", exception);
      throw exception;
    }
  }
}
