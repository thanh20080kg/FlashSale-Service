package com.shiro.warehouse.dto;

import com.shiro.warehouse.constants.WareHouseOperation;
import java.util.UUID;

public record WarehouseRequest(
    WareHouseOperation operation, UUID reservationKey, UUID productId, long quantity) {}
