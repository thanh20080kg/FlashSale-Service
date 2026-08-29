package com.shiro.flashsale.entity;

import com.shiro.flashsale.constants.AuthChannel;
import com.shiro.flashsale.constants.Role;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

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

  public UUID getId() {
    return id;
  }

  public AuthChannel getChannel() {
    return channel;
  }

  public String getIdentifier() {
    return identifier;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public boolean isVerified() {
    return verified;
  }

  public Role getRole() {
    return role;
  }

  public void verify() {
    this.verified = true;
  }

  /** Role changes are an administrative action, never something a request payload can drive. */
  public void assignRole(Role role) {
    this.role = role;
  }
}
