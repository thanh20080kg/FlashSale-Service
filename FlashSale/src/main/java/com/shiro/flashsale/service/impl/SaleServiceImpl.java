package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.constants.RedisKeyConstants;
import com.shiro.flashsale.dto.SaleDtos;
import com.shiro.flashsale.entity.FlashSaleItem;
import com.shiro.flashsale.entity.FlashSaleItemQuota;
import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import com.shiro.flashsale.repository.FlashSaleItemRepository;
import com.shiro.flashsale.repository.PurchaseRepository;
import com.shiro.flashsale.service.PurchaseExecuter;
import com.shiro.flashsale.service.RedisService;
import com.shiro.flashsale.service.SaleService;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {
  private static final Logger log = LoggerFactory.getLogger(SaleServiceImpl.class);
  private final FlashSaleItemRepository items;
  private final FlashSaleItemQuotaRepository quotas;
  private final PurchaseRepository purchases;
  private final FlashSaleQuotaService quotaService;
  private final PurchaseExecuter executor;
  private final RedisService redisService;
  private final ObjectMapper objectMapper;
  private final AppProperties properties;

  @Override
  public List<SaleDtos.SaleItemResponse> currentItems() {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();
    String cacheKey =
        RedisKeyConstants.SALE_CURRENT + today + ":" + now.getHour() + ":" + now.getMinute();

    List<SaleDtos.SaleItemResponse> cached = readCache(cacheKey);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    List<SaleDtos.SaleItemResponse> response = loadCurrentItems(today, now);
    writeCache(cacheKey, response);
    return response;
  }

  @Override
  public SaleDtos.PurchaseResponse purchase(UUID userId, SaleDtos.PurchaseRequest request) {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();
    String dailyLimitKey = RedisKeyConstants.DAILY_LIMIT + today + ":" + userId;
    String itemQuotaKey = RedisKeyConstants.ITEM_QUOTA + today + ":" + request.itemId();

    validatePrePurchase(dailyLimitKey, itemQuotaKey);
    try {
      FlashSaleItem item =
          items
              .findActiveById(request.itemId(), today, now)
              .orElseThrow(() -> ApiException.of(ErrorCode.SALE_NOT_ACTIVE));
      return executor.execute(userId, item, today);
    } catch (RuntimeException failure) {
      rollbackDailyLimit(dailyLimitKey);
      refundQuota(itemQuotaKey);
      throw failure;
    }
  }

  @Override
  public void reloadQuota() {
    ZonedDateTime current = ZonedDateTime.now(properties.getTimezone());
    LocalDate saleDate = current.toLocalDate();
    LocalTime saleTime = current.toLocalTime();

    Date currentDateTime = new Date();
    items
        .findCurrent(saleDate, saleTime)
        .forEach(
            item -> {
              quotaService.ensureQuotaForToday(item.getId(), saleDate, item.getQuantity());
              long remaining =
                  quotas
                      .findByFlashSaleItemIdAndSaleDate(item.getId(), saleDate)
                      .map(FlashSaleItemQuota::getRemainingQuantity)
                      .orElse(item.getQuantity());
              redisService.set(
                  RedisKeyConstants.ITEM_QUOTA + saleDate + ":" + item.getId(),
                  String.valueOf(remaining),
                  ttlUntilNextMidnight(current.toInstant()));
            });
    System.out.println("process time : " + ((new Date()).getTime() - currentDateTime.getTime()));
  }

  private void validatePrePurchase(String dailyLimitKey, String itemQuotaKey) {
    Long purchaseCount = dailyPurchaseCount(dailyLimitKey);
    Long consumeQuota = consumeQuota(itemQuotaKey);
    if (purchaseCount > properties.getSale().getLimitDailyPurchase()) {
      rollbackDailyLimit(dailyLimitKey);
      refundQuota(itemQuotaKey);
      throw ApiException.of(ErrorCode.DAILY_LIMIT_REACHED);
    }
    if (consumeQuota < 0L) {
      rollbackDailyLimit(dailyLimitKey);
      refundQuota(itemQuotaKey);
      throw ApiException.of(ErrorCode.SOLD_OUT);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<SaleDtos.PurchaseHistoryResponse> purchaseHistory(UUID userId, int limit) {
    int pageSize = Math.max(1, Math.min(limit, 100));
    return purchases.findHistory(userId, PageRequest.of(0, pageSize)).stream()
        .map(
            p ->
                new SaleDtos.PurchaseHistoryResponse(
                    p.getId(),
                    p.getItem().getId(),
                    p.getItem().getProduct().getSku(),
                    p.getItem().getProduct().getName(),
                    p.getAmount(),
                    p.getPurchaseDate(),
                    p.getCreatedAt()))
        .toList();
  }

  private List<SaleDtos.SaleItemResponse> loadCurrentItems(LocalDate today, LocalTime now) {
    List<FlashSaleItem> active = items.findCurrent(today, now);
    if (active.isEmpty()) {
      return List.of();
    }

    Map<UUID, Long> remaining =
        quotas
            .findByFlashSaleItemIdInAndSaleDate(
                active.stream().map(FlashSaleItem::getId).toList(), today)
            .stream()
            .collect(
                Collectors.toMap(
                    FlashSaleItemQuota::getFlashSaleItemId,
                    FlashSaleItemQuota::getRemainingQuantity));

    return active.stream()
        .map(
            i ->
                new SaleDtos.SaleItemResponse(
                    i.getId(),
                    i.getProduct().getId(),
                    i.getProduct().getSku(),
                    i.getProduct().getName(),
                    i.getAmount(),
                    i.getQuantity(),
                    // No quota row yet means nobody has bought today: the full allowance is open.
                    remaining.getOrDefault(i.getId(), i.getQuantity()),
                    i.getSlot().getId(),
                    i.getSlot().getName(),
                    i.getSlot().getStartTime(),
                    i.getSlot().getEndTime(),
                    i.getSlot().isOvernight()))
        .toList();
  }

  private List<SaleDtos.SaleItemResponse> readCache(String key) {
    try {
      String raw = redisService.get(key);
      if (ObjectUtils.isEmpty(raw)) {
        return null;
      }
      return objectMapper.readValue(raw, new TypeReference<>() {});
    } catch (Exception ex) {
      // The cache is an optimization, never a dependency: fall through to the database.
      log.debug("Flash sale listing cache read failed, serving from database", ex);
      return null;
    }
  }

  private void writeCache(String key, List<SaleDtos.SaleItemResponse> value) {
    try {
      redisService.set(
          key,
          objectMapper.writeValueAsString(value),
          properties.getSale().getCurrentItemsCacheTtl());
    } catch (Exception ex) {
      log.debug("Flash sale listing cache write failed", ex);
    }
  }

  private Duration ttlUntilNextMidnight(Instant now) {
    ZonedDateTime nextMidnight =
        ZonedDateTime.now(properties.getTimezone())
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(properties.getTimezone());
    return Duration.between(now, nextMidnight.toInstant());
  }

  private Long dailyPurchaseCount(String key) {
    Long count = redisService.increment(key);
    if (count == 1L) {
      redisService.expire(key, ttlUntilNextMidnight(Instant.now()));
    }
    return count;
  }

  private void rollbackDailyLimit(String key) {
    redisService.decrement(key);
  }

  private Long consumeQuota(String key) {
    return redisService.decrement(key);
  }

  private void refundQuota(String key) {
    redisService.increment(key);
  }
}
