package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.dto.SaleDtos;
import com.shiro.flashsale.entity.Customer;
import com.shiro.flashsale.entity.FlashSaleItem;
import com.shiro.flashsale.entity.FlashSaleItemQuota;
import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import com.shiro.flashsale.repository.CustomerRepository;
import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import com.shiro.flashsale.repository.FlashSaleItemRepository;
import com.shiro.flashsale.repository.PurchaseRepository;
import com.shiro.flashsale.service.SaleService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class SaleServiceImpl implements SaleService {
  private static final Logger log = LoggerFactory.getLogger(SaleServiceImpl.class);
  private static final String LISTING_CACHE_KEY = "sale:current:";

  private final FlashSaleItemRepository items;
  private final FlashSaleItemQuotaRepository quotas;
  private final PurchaseRepository purchases;
  private final CustomerRepository customers;
  private final FlashSaleQuotaService quotaService;
  private final PurchaseExecutor executor;
  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final AppProperties properties;

  public SaleServiceImpl(
      FlashSaleItemRepository items,
      FlashSaleItemQuotaRepository quotas,
      PurchaseRepository purchases,
      CustomerRepository customers,
      FlashSaleQuotaService quotaService,
      PurchaseExecutor executor,
      StringRedisTemplate redis,
      ObjectMapper objectMapper,
      AppProperties properties) {
    this.items = items;
    this.quotas = quotas;
    this.purchases = purchases;
    this.customers = customers;
    this.quotaService = quotaService;
    this.executor = executor;
    this.redis = redis;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  /**
   * Read path for the storefront. The result is shared by every caller in the same second, so it is
   * cached in Redis (shared, not per-instance) for a very short TTL: it absorbs the read fan-out of
   * a sale without letting the displayed stock drift meaningfully.
   */
  @Override
  public List<SaleDtos.SaleItemResponse> currentItems() {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();
    String cacheKey = LISTING_CACHE_KEY + today + ":" + now.getHour() + ":" + now.getMinute();

    List<SaleDtos.SaleItemResponse> cached = readCache(cacheKey);
    if (cached != null) return cached;

    List<SaleDtos.SaleItemResponse> response = loadCurrentItems(today, now);
    writeCache(cacheKey, response);
    return response;
  }

  /** Both queries join-fetch what they need, so nothing is lazily resolved after the call. */
  private List<SaleDtos.SaleItemResponse> loadCurrentItems(LocalDate today, LocalTime now) {
    List<FlashSaleItem> active = items.findCurrent(now);
    if (active.isEmpty()) return List.of();

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

  @Override
  public SaleDtos.PurchaseResponse purchase(UUID userId, SaleDtos.PurchaseRequest request) {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();

    FlashSaleItem item =
        items
            .findActiveById(request.itemId(), now)
            .orElseThrow(() -> ApiException.of(ErrorCode.SALE_NOT_ACTIVE));

    // Outside the purchase transaction: creating today's counter must not extend the hot path's
    // lock window, and it is idempotent so racing callers converge on the same row.
    quotaService.ensureQuotaForToday(item.getId(), today, item.getQuantity());

    return executor.execute(userId, item, today);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SaleDtos.PurchaseHistoryResponse> purchaseHistory(UUID userId, int limit) {
    Customer customer = requireCustomer(userId);
    int pageSize = Math.max(1, Math.min(limit, 100));
    return purchases.findHistory(customer.getId(), PageRequest.of(0, pageSize)).stream()
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

  @Override
  @Transactional(readOnly = true)
  public SaleDtos.BalanceResponse balance(UUID userId) {
    Customer customer = requireCustomer(userId);
    return new SaleDtos.BalanceResponse(
        customer.getId(), customer.getDisplayName(), customer.getBalance());
  }

  private Customer requireCustomer(UUID userId) {
    return customers
        .findByUserId(userId)
        .orElseThrow(() -> ApiException.of(ErrorCode.CUSTOMER_NOT_FOUND));
  }

  private List<SaleDtos.SaleItemResponse> readCache(String key) {
    try {
      String raw = redis.opsForValue().get(key);
      if (raw == null) return null;
      return objectMapper.readValue(raw, new TypeReference<List<SaleDtos.SaleItemResponse>>() {});
    } catch (Exception ex) {
      // The cache is an optimisation, never a dependency: fall through to the database.
      log.debug("Flash sale listing cache read failed, serving from database", ex);
      return null;
    }
  }

  private void writeCache(String key, List<SaleDtos.SaleItemResponse> value) {
    try {
      redis
          .opsForValue()
          .set(
              key,
              objectMapper.writeValueAsString(value),
              properties.getSale().getCurrentItemsCacheTtl());
    } catch (Exception ex) {
      log.debug("Flash sale listing cache write failed", ex);
    }
  }
}
