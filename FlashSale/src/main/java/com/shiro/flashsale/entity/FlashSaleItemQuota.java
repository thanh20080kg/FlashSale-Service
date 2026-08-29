package com.shiro.flashsale.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Per-day remaining quota of a flash sale item.
 *
 * <p>Slots recur every day, so the allowance has to recur with them. Keeping the counter in its own
 * {@code (item, sale_date)} row means yesterday's sell-out never blocks today, and the row is
 * created lazily on first purchase of the day rather than by a batch job that could miss a run.
 */
@Entity
@Table(
    name = "flash_sale_item_quotas",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_item_sale_date",
            columnNames = {"flash_sale_item_id", "sale_date"}))
public class FlashSaleItemQuota {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "flash_sale_item_id", nullable = false, length = 36)
  private UUID flashSaleItemId;

  @Column(name = "sale_date", nullable = false)
  private LocalDate saleDate;

  @Column(name = "total_quantity", nullable = false)
  private long totalQuantity;

  @Column(name = "remaining_quantity", nullable = false)
  private long remainingQuantity;

  protected FlashSaleItemQuota() {}

  public FlashSaleItemQuota(UUID flashSaleItemId, LocalDate saleDate, long totalQuantity) {
    this.flashSaleItemId = flashSaleItemId;
    this.saleDate = saleDate;
    this.totalQuantity = totalQuantity;
    this.remainingQuantity = totalQuantity;
  }

  public UUID getId() {
    return id;
  }

  public UUID getFlashSaleItemId() {
    return flashSaleItemId;
  }

  public LocalDate getSaleDate() {
    return saleDate;
  }

  public long getTotalQuantity() {
    return totalQuantity;
  }

  public long getRemainingQuantity() {
    return remainingQuantity;
  }
}
