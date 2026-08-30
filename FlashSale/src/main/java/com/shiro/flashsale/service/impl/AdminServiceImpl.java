package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.constants.InventoryEventStatus;
import com.shiro.flashsale.constants.InventoryEventType;
import com.shiro.flashsale.dto.AdminDtos;
import com.shiro.flashsale.entity.Customer;
import com.shiro.flashsale.entity.FlashSaleItem;
import com.shiro.flashsale.entity.FlashSaleSlot;
import com.shiro.flashsale.entity.Inventory;
import com.shiro.flashsale.entity.Product;
import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import com.shiro.flashsale.repository.*;
import com.shiro.flashsale.service.AdminService;
import com.shiro.flashsale.service.InventorySyncService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminServiceImpl implements AdminService {
  private final ProductRepository products;
  private final InventoryRepository inventories;
  private final InventoryMovementRepository movements;
  private final InventoryEventRepository events;
  private final FlashSaleSlotRepository slots;
  private final FlashSaleItemRepository items;
  private final CustomerRepository customers;
  private final InventorySyncService sync;

  public AdminServiceImpl(
      ProductRepository products,
      InventoryRepository inventories,
      InventoryMovementRepository movements,
      InventoryEventRepository events,
      FlashSaleSlotRepository slots,
      FlashSaleItemRepository items,
      CustomerRepository customers,
      InventorySyncService sync) {
    this.products = products;
    this.inventories = inventories;
    this.movements = movements;
    this.events = events;
    this.slots = slots;
    this.items = items;
    this.customers = customers;
    this.sync = sync;
  }

  // ---------------------------------------------------------------- products

  private static AdminDtos.ProductResponse toProductResponse(
      Product product, long available, long sold) {
    return new AdminDtos.ProductResponse(
        product.getId(), product.getSku(), product.getName(), product.isActive(), available, sold);
  }

  private static AdminDtos.SlotResponse toSlotResponse(FlashSaleSlot slot) {
    return new AdminDtos.SlotResponse(
        slot.getId(),
        slot.getName(),
        slot.getStartTime(),
        slot.getEndTime(),
        slot.isActive(),
        slot.isOvernight());
  }

  private static AdminDtos.ItemResponse toItemResponse(FlashSaleItem item) {
    return new AdminDtos.ItemResponse(
        item.getId(),
        item.getSlot().getId(),
        item.getSlot().getName(),
        item.getProduct().getId(),
        item.getProduct().getSku(),
        item.getAmount(),
        item.getQuantity(),
        item.isActive());
  }

  // ---------------------------------------------------------------- inventory

  @Override
  @Transactional
  public AdminDtos.ProductResponse createProduct(AdminDtos.CreateProductRequest request) {
    if (products.existsBySku(request.sku())) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "SKU already exists");
    }

    Product product = products.save(new Product(request.sku(), request.name()));
    Inventory inventory = inventories.save(new Inventory(product, request.initialStock()));
    sync.enqueue(
        "PRODUCT_CREATED:" + product.getId(),
        InventoryEventType.STOCK_ADJUSTED,
        product.getId(),
        request.initialStock());
    return toProductResponse(
        product, inventory.getAvailableQuantity(), inventory.getSoldQuantity());
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminDtos.ProductResponse> listProducts() {
    return products.findAll().stream()
        .map(
            p ->
                inventories
                    .findByProductId(p.getId())
                    .map(i -> toProductResponse(p, i.getAvailableQuantity(), i.getSoldQuantity()))
                    .orElseGet(() -> toProductResponse(p, 0, 0)))
        .toList();
  }

  // ---------------------------------------------------------------- slots

  @Override
  @Transactional
  public AdminDtos.ProductResponse updateProduct(
      UUID productId, AdminDtos.UpdateProductRequest request) {
    Product product = requireProduct(productId);
    if (ObjectUtils.isNotEmpty(request.name()) && !request.name().isBlank()) {
      product.setName(request.name());
    }

    if (ObjectUtils.isNotEmpty(request.active())
        && ObjectUtils.notEqual(request.active(), product.isActive())) {
      product.setActive(request.active());
      if (!request.active()) {
        // Deactivating a product has to reach flash sale state too. Routing it through the outbox
        // means the follow-up happens exactly once, even if this instance dies right after commit.
        String eventKey = "PRODUCT_DEACTIVATED:" + productId + ":" + LocalDate.now();
        if (!events.existsByEventKey(eventKey)) {
          sync.enqueue(eventKey, InventoryEventType.PRODUCT_DEACTIVATED, productId, 0);
        }
      }
    }
    products.save(product);
    Inventory inventory = requireInventory(productId);
    return toProductResponse(
        product, inventory.getAvailableQuantity(), inventory.getSoldQuantity());
  }

  @Override
  @Transactional
  public AdminDtos.ProductResponse adjustInventory(
      UUID productId, AdminDtos.AdjustInventoryRequest request) {
    Product product = requireProduct(productId);
    if (request.delta() == 0) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "delta must not be zero");
    }

    // Guarded atomic UPDATE: stock can be corrected concurrently and still never goes negative.
    if (inventories.adjust(productId, request.delta()) == 0) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "Adjustment would drive stock negative");
    }

    // The reference is the idempotency key: replaying the same operator action is a no-op.
    String eventKey = "STOCK_ADJUST:" + productId + ":" + request.reference();
    if (events.existsByEventKey(eventKey)) {
      throw new ApiException(
          ErrorCode.DUPLICATE_RESOURCE, "This adjustment reference was already applied");
    }
    sync.enqueue(eventKey, InventoryEventType.STOCK_ADJUSTED, productId, request.delta());

    Inventory inventory = requireInventory(productId);
    return toProductResponse(
        product, inventory.getAvailableQuantity(), inventory.getSoldQuantity());
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminDtos.InventoryMovementResponse> inventoryMovements(UUID productId, int limit) {
    int pageSize = Math.max(1, Math.min(limit, 200));
    return movements
        .findByProductIdOrderByCreatedAtDesc(productId, PageRequest.of(0, pageSize))
        .stream()
        .map(
            m ->
                new AdminDtos.InventoryMovementResponse(
                    m.getId(),
                    m.getProductId(),
                    m.getEventType().name(),
                    m.getQuantityDelta(),
                    m.getBalanceAfter(),
                    m.getCreatedAt()))
        .toList();
  }

  // ---------------------------------------------------------------- flash sale items

  @Override
  @Transactional
  public AdminDtos.SlotResponse createSlot(AdminDtos.CreateSlotRequest request) {
    FlashSaleSlot slot =
        slots.save(new FlashSaleSlot(request.name(), request.startTime(), request.endTime()));
    return toSlotResponse(slot);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminDtos.SlotResponse> listSlots() {
    return slots.findAllByOrderByStartTimeAsc().stream()
        .map(AdminServiceImpl::toSlotResponse)
        .toList();
  }

  @Override
  @Transactional
  public AdminDtos.SlotResponse updateSlot(UUID slotId, AdminDtos.UpdateSlotRequest request) {
    FlashSaleSlot slot =
        slots.findById(slotId).orElseThrow(() -> ApiException.of(ErrorCode.SLOT_NOT_FOUND));
    if (ObjectUtils.isNotEmpty(request.active())) {
      slot.setActive(request.active());
    }
    return toSlotResponse(slots.save(slot));
  }

  // ---------------------------------------------------------------- customers / ops

  @Override
  @Transactional
  public AdminDtos.ItemResponse createItem(UUID slotId, AdminDtos.CreateItemRequest request) {
    FlashSaleSlot slot =
        slots.findById(slotId).orElseThrow(() -> ApiException.of(ErrorCode.SLOT_NOT_FOUND));
    Product product = requireProduct(request.productId());
    FlashSaleItem item =
        items.save(new FlashSaleItem(slot, product, request.amount(), request.quantity()));
    return toItemResponse(item);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminDtos.ItemResponse> listItems() {
    return items.findAllWithRelations().stream().map(AdminServiceImpl::toItemResponse).toList();
  }

  // ---------------------------------------------------------------- helpers

  /**
   * Changing quantity affects tomorrow's allowance. Today's counter is already materialised and is
   * intentionally left alone, so an edit mid-sale cannot retroactively oversell.
   */
  @Override
  @Transactional
  public AdminDtos.ItemResponse updateItem(UUID itemId, AdminDtos.UpdateItemRequest request) {
    FlashSaleItem item =
        items.findById(itemId).orElseThrow(() -> ApiException.of(ErrorCode.ITEM_NOT_FOUND));
    if (ObjectUtils.isNotEmpty(request.amount())) {
      item.setAmount(request.amount());
    }
    if (ObjectUtils.isNotEmpty(request.quantity())) {
      item.setQuantity(request.quantity());
    }
    if (ObjectUtils.isNotEmpty(request.active())) {
      item.setActive(request.active());
    }
    return toItemResponse(items.save(item));
  }

  @Override
  @Transactional
  public AdminDtos.CustomerResponse topUp(UUID userId, AdminDtos.TopUpRequest request) {
    Customer customer =
        customers
            .findByUserId(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.CUSTOMER_NOT_FOUND));
    customers.credit(customer.getId(), request.amount());
    Customer refreshed =
        customers
            .findByUserId(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.CUSTOMER_NOT_FOUND));
    return new AdminDtos.CustomerResponse(
        refreshed.getId(), userId, refreshed.getDisplayName(), refreshed.getBalance());
  }

  @Override
  @Transactional(readOnly = true)
  public AdminDtos.SyncStatusResponse syncStatus() {
    return new AdminDtos.SyncStatusResponse(
        events.countByStatus(InventoryEventStatus.PENDING),
        events.countByStatus(InventoryEventStatus.PROCESSED),
        events.countByStatus(InventoryEventStatus.FAILED));
  }

  private Product requireProduct(UUID productId) {
    return products
        .findById(productId)
        .orElseThrow(() -> ApiException.of(ErrorCode.PRODUCT_NOT_FOUND));
  }

  private Inventory requireInventory(UUID productId) {
    return inventories
        .findByProductId(productId)
        .orElseThrow(() -> ApiException.of(ErrorCode.PRODUCT_NOT_FOUND));
  }
}
