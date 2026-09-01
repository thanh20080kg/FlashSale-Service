package com.shiro.warehouse.service;

import com.shiro.warehouse.dto.WarehouseRequest;
import com.shiro.warehouse.dto.WarehouseResponse;

public interface WarehouseService {
  WarehouseResponse reserve(WarehouseRequest command);

  WarehouseResponse release(WarehouseRequest command);

  WarehouseResponse sold(WarehouseRequest command);

  void syncPurchaseStatuses();
}
