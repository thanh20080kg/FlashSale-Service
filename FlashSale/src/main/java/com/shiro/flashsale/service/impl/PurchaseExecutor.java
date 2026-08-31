package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.constants.InventoryEventType;
import com.shiro.flashsale.dto.SaleDtos;
import com.shiro.flashsale.entity.Customer;
import com.shiro.flashsale.entity.FlashSaleItem;
import com.shiro.flashsale.entity.InventoryEvent;
import com.shiro.flashsale.entity.Purchase;
import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import com.shiro.flashsale.repository.CustomerRepository;
import com.shiro.flashsale.repository.FlashSaleItemQuotaRepository;
import com.shiro.flashsale.repository.InventoryEventRepository;
import com.shiro.flashsale.repository.InventoryRepository;
import com.shiro.flashsale.repository.PurchaseRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PurchaseExecutor {
  private final CustomerRepository customers;
  private final PurchaseRepository purchases;
  private final InventoryRepository inventory;
  private final FlashSaleItemQuotaRepository quotas;
  private final InventoryEventRepository events;
  private final EntityManager entityManager;

  @Transactional
  public SaleDtos.PurchaseResponse execute(UUID userId, FlashSaleItem item, LocalDate saleDate) {
    Instant now = Instant.now();
    Customer customer =
        customers
            .findByUserId(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.CUSTOMER_NOT_FOUND));

    // Fast path only. The unique key below is the actual guarantee.
    if (purchases.existsByCustomerIdAndPurchaseDate(customer.getId(), saleDate)) {
      throw ApiException.of(ErrorCode.DAILY_LIMIT_REACHED);
    }

    // The item was read outside this transaction; reference it by id rather than re-attaching it.
    FlashSaleItem itemRef = entityManager.getReference(FlashSaleItem.class, item.getId());
    Purchase purchase = new Purchase(customer, itemRef, item.getAmount(), saleDate, now);
    try {
      purchases.saveAndFlush(purchase);
    } catch (DataIntegrityViolationException duplicateForToday) {
      // Two concurrent requests for the same customer on the same day; the index rejected one.
      throw ApiException.of(ErrorCode.DAILY_LIMIT_REACHED);
    }

    // --- contended rows, kept last ---
    if (inventory.reserve(item.getProduct().getId()) == 0) {
      throw ApiException.of(ErrorCode.OUT_OF_STOCK);
    }
    if (quotas.decrement(item.getId(), 1, saleDate) == 0) {
      throw ApiException.of(ErrorCode.SOLD_OUT);
    }

    // Outbox row, same transaction as the purchase: the event exists exactly when the sale does.
    events.saveAndFlush(
        new InventoryEvent(
            "PURCHASE:" + purchase.getId(),
            InventoryEventType.ORDER_RESERVED,
            item.getProduct().getId(),
            -1,
            now));

    if (customers.debit(customer.getId(), item.getAmount()) == 0) {
      throw ApiException.of(ErrorCode.INSUFFICIENT_BALANCE);
    }

    // Outbox row, same transaction as the purchase: the event exists exactly when the sale does.
    events.saveAndFlush(
        new InventoryEvent(
            "PURCHASE:" + purchase.getId(),
            InventoryEventType.ORDER_SOLD,
            item.getProduct().getId(),
            -1,
            now));

    return new SaleDtos.PurchaseResponse(
        purchase.getId(),
        item.getId(),
        item.getProduct().getSku(),
        item.getAmount(),
        saleDate,
        "Purchase successful");
  }
}
