package com.shiro.flashsale.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;

@Getter
@Entity
@Table(
    name = "flash_sale_slots",
    indexes = @Index(name = "idx_slot_sale_date", columnList = "sale_date"))
public class FlashSaleSlot {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "owner_id")
  private UUID ownerId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false)
  private LocalTime startTime;

  @Column(nullable = false)
  private LocalTime endTime;

  @Column(name = "sale_date")
  private LocalDate saleDate;

  @Column(nullable = false)
  private boolean active = true;

  protected FlashSaleSlot() {}

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
