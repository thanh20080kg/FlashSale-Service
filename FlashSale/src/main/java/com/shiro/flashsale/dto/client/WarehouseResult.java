package com.shiro.flashsale.dto.client;

import com.shiro.flashsale.constants.WarehouseStatus;

public record WarehouseResult(boolean success, WarehouseStatus status, String message) {}
