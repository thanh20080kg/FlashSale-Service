package com.shiro.authentication.entity;

import com.shiro.authentication.constants.AuthChannel;
import com.shiro.authentication.constants.OtpPurpose;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
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
  @JdbcTypeCode(SqlTypes.CHAR)
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
}
