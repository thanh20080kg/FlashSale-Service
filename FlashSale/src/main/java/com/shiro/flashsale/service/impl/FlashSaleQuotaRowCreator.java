package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.entity.FlashSaleItemQuota;
import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Creates the daily quota records required by flash-sale items. */
@Service
@AllArgsConstructor
public class FlashSaleQuotaRowCreator {
  private final FlashSaleItemQuotaRepository quotas;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void create(UUID itemId, LocalDate saleDate, long quantity) {
    quotas.saveAndFlush(new FlashSaleItemQuota(itemId, saleDate, quantity));
  }
}
