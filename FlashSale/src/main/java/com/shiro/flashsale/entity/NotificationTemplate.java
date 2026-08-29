package com.shiro.flashsale.entity;

import com.shiro.flashsale.constants.NotificationType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "notification_templates",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_notification_template_code", columnNames = "code"))
public class NotificationTemplate {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String code;

  @Lob
  @Column(nullable = false)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NotificationType type;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected NotificationTemplate() {}

  public NotificationTemplate(String code, String content, NotificationType type) {
    this.code = code;
    this.content = content;
    this.type = type;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public String getCode() {
    return code;
  }

  public String getContent() {
    return content;
  }

  public NotificationType getType() {
    return type;
  }
}
