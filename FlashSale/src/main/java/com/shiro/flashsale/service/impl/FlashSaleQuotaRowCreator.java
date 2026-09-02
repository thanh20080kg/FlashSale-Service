package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.entity.FlashSaleItemQuota;
import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the daily quota row in a transaction of its own.
 *
 * <p>It is a separate bean on purpose: when two instances race to create the same row one of them
 * hits the unique key, and the failure has to roll back <em>only</em> this insert. Letting the
 * exception cross a transaction boundary keeps the caller's persistence context clean.
 */
@Service
@AllArgsConstructor
public class FlashSaleQuotaRowCreator {
  private final FlashSaleItemQuotaRepository quotas;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void create(UUID itemId, LocalDate saleDate, long quantity) {
    quotas.saveAndFlush(new FlashSaleItemQuota(itemId, saleDate, quantity));
  }
}
