package com.shiro.authentication.repository;

import com.shiro.authentication.constants.OtpPurpose;
import com.shiro.authentication.entity.OtpChallenge;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {
  Optional<OtpChallenge> findTopByIdentifierAndPurposeAndConsumedFalseOrderByExpiresAtDesc(
      String identifier, OtpPurpose purpose);

  @Modifying(clearAutomatically = true)
  @Query(
      value =
          "UPDATE otp_challenges SET consumed = TRUE WHERE identifier = :identifier AND purpose = :purpose AND consumed = FALSE",
      nativeQuery = true)
  int consumeOpenChallenges(
      @Param("identifier") String identifier, @Param("purpose") String purpose);

  @Modifying
  @Query("delete from OtpChallenge o where o.expiresAt < :before")
  int deleteExpiredBefore(@Param("before") Instant before);

  @Modifying
  @Query(
      value =
          "UPDATE otp_challenges SET attempts = attempts + 1 WHERE id = :id AND consumed = FALSE",
      nativeQuery = true)
  int incrementAttempts(@Param("id") String id);

  @Modifying
  @Query(
      value = "UPDATE otp_challenges SET consumed = TRUE WHERE id = :id AND consumed = FALSE",
      nativeQuery = true)
  int consume(@Param("id") String id);
}
