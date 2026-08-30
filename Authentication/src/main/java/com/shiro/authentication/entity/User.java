package com.shiro.authentication.entity;

import com.shiro.authentication.constants.AuthChannel;
import com.shiro.authentication.constants.Role;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_user_identifier",
          columnNames = {"identifier"})
    })
public class User {
  @Getter
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthChannel channel;

  @Getter
  @Column(nullable = false, length = 190)
  private String identifier;

  @Getter
  @Column(nullable = false)
  private String passwordHash;

  @Getter
  @Column(nullable = false)
  private boolean verified;

  @Getter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Column(nullable = false)
  private Instant createdAt;

  protected User() {}

  public User(AuthChannel channel, String identifier, String passwordHash) {
    this.channel = channel;
    this.identifier = identifier;
    this.passwordHash = passwordHash;
    this.verified = false;
    this.role = Role.BUYER;
    this.createdAt = Instant.now();
  }

  public void verify() {
    this.verified = true;
  }

  /** Role changes are an administrative action, never something a request payload can drive. */
  public void assignRole(Role role) {
    this.role = role;
  }
}
