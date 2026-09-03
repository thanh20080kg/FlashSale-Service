package com.shiro.flashsale.service;

import com.shiro.flashsale.dto.client.WarehouseDtos;
import java.util.UUID;

public interface WarehouseService {
  WarehouseDtos.Response reserve(UUID productId, UUID reservationKey, Integer quantity);

  WarehouseDtos.Response sold(UUID productId, UUID reservationKey);

  WarehouseDtos.Response release(UUID reservationKey, UUID productId);
}
