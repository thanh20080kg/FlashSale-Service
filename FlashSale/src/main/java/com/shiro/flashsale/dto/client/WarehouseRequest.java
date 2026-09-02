package com.shiro.flashsale.dto.client;

import com.shiro.flashsale.constants.WareHouseOperation;
import java.util.UUID;

public record WarehouseRequest(
    WareHouseOperation operation, String reservationKey, UUID productId, long quantity) {}
