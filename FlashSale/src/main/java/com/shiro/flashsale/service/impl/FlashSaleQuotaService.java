package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FlashSaleQuotaService {
  private static final Logger log = LoggerFactory.getLogger(FlashSaleQuotaService.class);
  private final FlashSaleItemQuotaRepository quotas;
  private final FlashSaleQuotaRowCreator creator;

  public void ensureQuotaForToday(UUID itemId, LocalDate saleDate, long quantity) {
    if (quotas.existsByFlashSaleItemIdAndSaleDate(itemId, saleDate)) {
      return;
    }
    try {
      creator.create(itemId, saleDate, quantity);
    } catch (DataIntegrityViolationException alreadyCreatedConcurrently) {
      log.debug("Quota row was created concurrently, itemId={}, saleDate={}", itemId, saleDate);
      // Another request (or another instance) won the race. The row now exists, which is all
      // this method promises.
    }
  }
}
