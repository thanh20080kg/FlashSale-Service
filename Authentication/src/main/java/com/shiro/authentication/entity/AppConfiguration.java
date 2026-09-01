package com.shiro.authentication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "app_configuration")
public class AppConfiguration {
  @Id
  @Column(name = "config_key", nullable = false, length = 190)
  private String key;

  @Column(name = "config_value", nullable = false, length = 500)
  private String value;

  protected AppConfiguration() {}
}
