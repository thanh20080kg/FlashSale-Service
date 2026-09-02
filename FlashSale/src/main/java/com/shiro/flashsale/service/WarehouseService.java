package com.shiro.flashsale.service;

import com.shiro.flashsale.dto.client.WarehouseResult;
import java.util.UUID;

public interface WarehouseService {
  WarehouseResult reserve(UUID productId, String reservationKey);

  WarehouseResult sold(UUID productId, String reservationKey);

  WarehouseResult release(String reservationKey, UUID productId);
}
