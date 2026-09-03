package com.shiro.warehouse.service;

import com.shiro.warehouse.dto.WarehouseDtos;

public interface WarehouseService {
  WarehouseDtos.Response reserve(WarehouseDtos.Request command);

  WarehouseDtos.Response release(WarehouseDtos.Request command);

  WarehouseDtos.Response sold(WarehouseDtos.Request command);

  void syncPurchaseStatuses();
}
