package com.shiro.flashsale.controller;

import com.shiro.flashsale.dto.AdminDtos;
import com.shiro.flashsale.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Flash sale configuration lives entirely in the database and is managed through these endpoints.
 * Authorisation is enforced by permission, not by role, so a new role only needs a mapping in
 * {@code RolePermissions} to gain or lose access.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
  private final AdminService service;

  public AdminController(AdminService service) {
    this.service = service;
  }

  // ---- products & inventory ----

  @PostMapping("/products")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminDtos.ProductResponse createProduct(
      @Valid @RequestBody AdminDtos.CreateProductRequest request) {
    return service.createProduct(request);
  }

  @GetMapping("/products")
  public List<AdminDtos.ProductResponse> listProducts() {
    return service.listProducts();
  }

  @PatchMapping("/products/{productId}")
  public AdminDtos.ProductResponse updateProduct(
      @PathVariable UUID productId, @Valid @RequestBody AdminDtos.UpdateProductRequest request) {
    return service.updateProduct(productId, request);
  }

  @PostMapping("/products/{productId}/inventory")
  public AdminDtos.ProductResponse adjustInventory(
      @PathVariable UUID productId, @Valid @RequestBody AdminDtos.AdjustInventoryRequest request) {
    return service.adjustInventory(productId, request);
  }

  @GetMapping("/products/{productId}/movements")
  public List<AdminDtos.InventoryMovementResponse> movements(
      @PathVariable UUID productId, @RequestParam(defaultValue = "50") int limit) {
    return service.inventoryMovements(productId, limit);
  }

  // ---- slots & items ----

  @PostMapping("/slots")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminDtos.SlotResponse createSlot(
      @Valid @RequestBody AdminDtos.CreateSlotRequest request) {
    return service.createSlot(request);
  }

  @GetMapping("/slots")
  public List<AdminDtos.SlotResponse> listSlots() {
    return service.listSlots();
  }

  @PatchMapping("/slots/{slotId}")
  public AdminDtos.SlotResponse updateSlot(
      @PathVariable UUID slotId, @Valid @RequestBody AdminDtos.UpdateSlotRequest request) {
    return service.updateSlot(slotId, request);
  }

  @PostMapping("/slots/{slotId}/items")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminDtos.ItemResponse createItem(
      @PathVariable UUID slotId, @Valid @RequestBody AdminDtos.CreateItemRequest request) {
    return service.createItem(slotId, request);
  }

  @GetMapping("/items")
  public List<AdminDtos.ItemResponse> listItems() {
    return service.listItems();
  }

  @PatchMapping("/items/{itemId}")
  public AdminDtos.ItemResponse updateItem(
      @PathVariable UUID itemId, @Valid @RequestBody AdminDtos.UpdateItemRequest request) {
    return service.updateItem(itemId, request);
  }

  // ---- customers & ops ----

  /** Requires PERM_USER_MANAGE, a strictly narrower grant than the catalogue endpoints. */
  @PostMapping("/customers/{userId}/balance")
  public AdminDtos.CustomerResponse topUp(
      @PathVariable UUID userId, @Valid @RequestBody AdminDtos.TopUpRequest request) {
    return service.topUp(userId, request);
  }

  @GetMapping("/inventory-sync/status")
  public AdminDtos.SyncStatusResponse syncStatus() {
    return service.syncStatus();
  }
}
