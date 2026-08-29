package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Lazily materialises the {@code (item, sale_date)} quota row before the purchase transaction. */
@Service
public class FlashSaleQuotaService {
  private final FlashSaleItemQuotaRepository quotas;
  private final FlashSaleQuotaRowCreator creator;

  public FlashSaleQuotaService(
      FlashSaleItemQuotaRepository quotas, FlashSaleQuotaRowCreator creator) {
    this.quotas = quotas;
    this.creator = creator;
  }

  public void ensureQuotaForToday(UUID itemId, LocalDate saleDate, long quantity) {
    if (quotas.existsByFlashSaleItemIdAndSaleDate(itemId, saleDate)) return;
    try {
      creator.create(itemId, saleDate, quantity);
    } catch (DataIntegrityViolationException alreadyCreatedConcurrently) {
      // Another request (or another instance) won the race. The row now exists, which is all
      // this method promises.
    }
  }
}
