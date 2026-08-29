package com.shiro.flashsale.service;

import com.shiro.flashsale.dto.AdminDtos;
import java.util.List;
import java.util.UUID;

/**
 * Operator-facing management of the catalogue and of flash sale configuration.
 *
 * <p>Everything here writes to the database - no flash sale schedule, price or quota is ever
 * expressed in code, so adding a product or a new window is an API call, not a deployment.
 */
public interface AdminService {

  AdminDtos.ProductResponse createProduct(AdminDtos.CreateProductRequest request);

  List<AdminDtos.ProductResponse> listProducts();

  AdminDtos.ProductResponse updateProduct(UUID productId, AdminDtos.UpdateProductRequest request);

  AdminDtos.ProductResponse adjustInventory(
      UUID productId, AdminDtos.AdjustInventoryRequest request);

  List<AdminDtos.InventoryMovementResponse> inventoryMovements(UUID productId, int limit);

  AdminDtos.SlotResponse createSlot(AdminDtos.CreateSlotRequest request);

  List<AdminDtos.SlotResponse> listSlots();

  AdminDtos.SlotResponse updateSlot(UUID slotId, AdminDtos.UpdateSlotRequest request);

  AdminDtos.ItemResponse createItem(UUID slotId, AdminDtos.CreateItemRequest request);

  List<AdminDtos.ItemResponse> listItems();

  AdminDtos.ItemResponse updateItem(UUID itemId, AdminDtos.UpdateItemRequest request);

  AdminDtos.CustomerResponse topUp(UUID userId, AdminDtos.TopUpRequest request);

  AdminDtos.SyncStatusResponse syncStatus();
}
