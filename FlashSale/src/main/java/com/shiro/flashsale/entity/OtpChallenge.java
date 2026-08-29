package com.shiro.flashsale.entity;

import com.shiro.flashsale.constants.AuthChannel;
import com.shiro.flashsale.constants.OtpPurpose;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "otp_challenges",
    indexes = {
      @Index(name = "idx_otp_lookup", columnList = "identifier,purpose,consumed"),
      @Index(name = "idx_otp_expiry", columnList = "expires_at")
    })
public class OtpChallenge {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthChannel channel;

  @Column(nullable = false)
  private String identifier;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OtpPurpose purpose;

  @Column(nullable = false)
  private String codeHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean consumed;

  /** Wrong-code counter. Guessing a 6-digit code is only feasible without one. */
  @Column(nullable = false)
  private int attempts;

  protected OtpChallenge() {}

  public OtpChallenge(AuthChannel c, String i, OtpPurpose p, String hash, Instant expires) {
    channel = c;
    identifier = i;
    purpose = p;
    codeHash = hash;
    expiresAt = expires;
    consumed = false;
    attempts = 0;
  }

  public UUID getId() {
    return id;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public boolean isConsumed() {
    return consumed;
  }

  public int getAttempts() {
    return attempts;
  }

  public void consume() {
    consumed = true;
  }

  public void registerFailedAttempt() {
    attempts++;
  }
}
