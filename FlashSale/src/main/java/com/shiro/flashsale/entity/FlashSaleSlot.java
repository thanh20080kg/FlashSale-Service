package com.shiro.flashsale.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Setter;

/**
 * A recurring daily time window. When {@code endTime <= startTime} the window wraps past midnight
 * (for example 22:00 -> 02:00); {@link #containsAt} and the repository query both honour that.
 */
@Entity
@Table(name = "flash_sale_slots")
public class FlashSaleSlot {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false)
  private LocalTime startTime;

  @Column(nullable = false)
  private LocalTime endTime;

  @Setter
  @Column(nullable = false)
  private boolean active = true;

  protected FlashSaleSlot() {}

  public FlashSaleSlot(String name, LocalTime startTime, LocalTime endTime) {
    this.name = name;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isOvernight() {
    return !endTime.isAfter(startTime);
  }

  public boolean containsAt(LocalTime time) {
    if (isOvernight()) {
      return !time.isBefore(startTime) || time.isBefore(endTime);
    }
    return !time.isBefore(startTime) && time.isBefore(endTime);
  }
}
