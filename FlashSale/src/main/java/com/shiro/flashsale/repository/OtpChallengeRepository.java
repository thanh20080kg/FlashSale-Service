package com.shiro.flashsale.repository;

import com.shiro.flashsale.constants.OtpPurpose;
import com.shiro.flashsale.entity.OtpChallenge;
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

  /** Issuing a new code retires every older one, so only the latest code can ever be redeemed. */
  @Modifying(clearAutomatically = true)
  @Query(
      """
      update OtpChallenge o set o.consumed = true
      where o.identifier = :identifier and o.purpose = :purpose and o.consumed = false
      """)
  int consumeOpenChallenges(
      @Param("identifier") String identifier, @Param("purpose") OtpPurpose purpose);

  @Modifying
  @Query("delete from OtpChallenge o where o.expiresAt < :before")
  int deleteExpiredBefore(@Param("before") Instant before);
}
