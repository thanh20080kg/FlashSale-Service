package com.shiro.warehouse.dto;

import com.shiro.warehouse.constants.OrderStatus;

public record WarehouseResponse(boolean success, OrderStatus status, String message) {}
