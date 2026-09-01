package com.shiro.authentication.entity;

import com.shiro.authentication.constants.AuthChannel;
import com.shiro.authentication.constants.Role;
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
    name = "users",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_user_identifier",
          columnNames = {"identifier"})
    })
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthChannel channel;

  @Column(nullable = false, length = 190)
  private String identifier;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private boolean verified;

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
}
