package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.constants.RedisKeyConstants;
import com.shiro.flashsale.dto.PurchaseStatusSyncDtos;
import com.shiro.flashsale.dto.SaleDtos;
import com.shiro.flashsale.entity.FlashSaleItem;
import com.shiro.flashsale.entity.FlashSaleItemQuota;
import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import com.shiro.flashsale.exception.PurchaseResponseException;
import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import com.shiro.flashsale.repository.FlashSaleItemRepository;
import com.shiro.flashsale.repository.PurchaseRepository;
import com.shiro.flashsale.service.CacheConfigService;
import com.shiro.flashsale.service.PurchaseExecuter;
import com.shiro.flashsale.service.PurchaseService;
import com.shiro.flashsale.service.RedisService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Provides flash-sale catalogue, purchase, quota, and history operations. */
@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {
  private static final Logger log = LoggerFactory.getLogger(PurchaseServiceImpl.class);
  private final FlashSaleItemRepository items;
  private final FlashSaleItemQuotaRepository quotas;
  private final PurchaseRepository purchases;
  private final FlashSaleQuotaService quotaService;
  private final PurchaseExecuter executor;
  private final RedisService redisService;
  private final CacheConfigService cacheConfigService;
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
    // Default quantity is 1
    var quantity =
        ObjectUtils.isEmpty(request.quantity())
            ? 1
            : request.quantity() > 0 ? request.quantity() : 1;

    validatePrePurchase(dailyLimitKey, itemQuotaKey, quantity);
    try {
      FlashSaleItem item =
          items
              .findActiveById(request.itemId(), today, now)
              .orElseThrow(() -> ApiException.of(ErrorCode.SALE_NOT_ACTIVE));
      return executor.execute(userId, item, quantity, today);
    } catch (RuntimeException failure) {
      log.error(
          "Purchase execution failed, userId={}, itemId={}", userId, request.itemId(), failure);
      rollBackRedis(dailyLimitKey, itemQuotaKey, quantity);
      if (failure instanceof PurchaseResponseException responseException) {
        return (SaleDtos.PurchaseResponse) responseException.getObject();
      }
      throw failure;
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  @Override
  public void reloadQuota() {
    ZonedDateTime current = ZonedDateTime.now(properties.getTimezone());
    LocalDate saleDate = current.toLocalDate();
    LocalTime saleTime = current.toLocalTime();

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
  }

  private void validatePrePurchase(String dailyLimitKey, String itemQuotaKey, int quantity) {
    try {
      Long purchaseCount = dailyPurchaseCount(dailyLimitKey, quantity);
      Long consumeQuota = consumeItemsQuota(itemQuotaKey, quantity);
      if (purchaseCount > cacheConfigService.getLimitDailyPurchase()) {
        throw ApiException.of(ErrorCode.DAILY_LIMIT_REACHED);
      }
      if (consumeQuota < 0L) {
        throw ApiException.of(ErrorCode.SOLD_OUT);
      }
    } catch (Exception e) {
      rollBackRedis(dailyLimitKey, itemQuotaKey, quantity);
      if (e instanceof ApiException apiException) {
        throw apiException;
      }
      throw ApiException.of(ErrorCode.INTERNAL_ERROR);
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
                    p.getStatus().name(),
                    p.getCreatedAt()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PurchaseStatusSyncDtos.Response getStatus(List<UUID> purchaseIds) {
    if (purchaseIds == null || purchaseIds.isEmpty()) {
      return new PurchaseStatusSyncDtos.Response(List.of());
    }
    List<PurchaseStatusSyncDtos.Entry> entries =
        purchases.findAllById(purchaseIds).stream()
            .map(p -> new PurchaseStatusSyncDtos.Entry(p.getId(), p.getStatus().name()))
            .toList();
    return new PurchaseStatusSyncDtos.Response(entries);
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

  private Long dailyPurchaseCount(String key, int quantity) {
    Long count = redisService.increment(key, quantity);
    if (count == quantity) {
      redisService.expire(key, ttlUntilNextMidnight(Instant.now()));
    }
    return count;
  }

  private Long consumeItemsQuota(String key, int quantity) {
    return redisService.decrement(key, quantity);
  }

  private void rollBackRedis(String dailyLimitKey, String itemQuotaKey, int quantity) {
    redisService.decrement(dailyLimitKey, quantity);
    redisService.increment(itemQuotaKey, quantity);
  }
}
