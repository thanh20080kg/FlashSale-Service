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
    String itemQuotasKey = RedisKeyConstants.ITEM_QUOTA + today + ":" + request.getItemId();
    // Default quantity is 1
    var quantity =
        ObjectUtils.isEmpty(request.getQuantity())
            ? 1
            : request.getQuantity() > 0 ? request.getQuantity() : 1;
    // Consume Redis quotas
    Long consumedQuotas = consumeRedisQuotas(itemQuotasKey, quantity);
    try {
      validatePrePurchase(consumedQuotas);
      FlashSaleItem item =
          items
              .findActiveById(request.getItemId(), today, now)
              .orElseThrow(() -> ApiException.of(ErrorCode.SALE_NOT_ACTIVE));
      return executor.execute(userId, item, quantity, today);
    } catch (RuntimeException failure) {
      log.error(
          "Purchase execution failed, userId={}, itemId={}", userId, request.getItemId(), failure);
      rollBackRedisQuotas(itemQuotasKey, quantity);
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

  private void validatePrePurchase(Long consumeQuota) {
    if (consumeQuota < 0L) {
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
                SaleDtos.PurchaseHistoryResponse.builder()
                    .purchaseId(p.getId())
                    .itemId(p.getItem().getId())
                    .sku(p.getItem().getProduct().getSku())
                    .productName(p.getItem().getProduct().getName())
                    .amount(p.getAmount())
                    .purchaseDate(p.getPurchaseDate())
                    .status(p.getStatus().name())
                    .createdAt(p.getCreatedAt())
                    .build())
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
            .map(p -> new PurchaseStatusSyncDtos.Entry(p.getStatus().name(), p.getId()))
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
                SaleDtos.SaleItemResponse.builder()
                    .itemId(i.getId())
                    .productId(i.getProduct().getId())
                    .sku(i.getProduct().getSku())
                    .productName(i.getProduct().getName())
                    .amount(i.getAmount())
                    .quantity(i.getQuantity())
                    // No quota row yet means nobody has bought today: the full allowance is open.
                    .remainingQuantity(remaining.getOrDefault(i.getId(), i.getQuantity()))
                    .slotId(i.getSlot().getId())
                    .slotName(i.getSlot().getName())
                    .startTime(i.getSlot().getStartTime())
                    .endTime(i.getSlot().getEndTime())
                    .overnight(i.getSlot().isOvernight())
                    .build())
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
          properties.getPurchase().getCurrentItemsCacheTtl());
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

  private Long consumeRedisQuotas(String key, int quantity) {
    return redisService.decrement(key, quantity);
  }

  private Long rollBackRedisQuotas(String itemQuotaKey, int quantity) {
    return redisService.increment(itemQuotaKey, quantity);
  }
}
