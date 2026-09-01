package com.shiro.warehouse.constants;


public enum OrderStatus {
    RESERVED ("reserved"),
    SOLD ("sold"),
    RELEASED ("released"),
    ALREADY_RESERVED ("idempotent"),
    ALREADY_SOLD ("idempotent"),
    ALREADY_RELEASED ("idempotent"),
    OUT_OF_STOCK("out of stock"),
    NOT_EXIST ("not exist"),
    INVALID ("unknown operation");

    public final String defaultMessage;

    OrderStatus(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public static OrderStatus fromString(String status) {
        for (OrderStatus orderStatus : OrderStatus.values()) {
            if (orderStatus.name().equalsIgnoreCase(status)) {
                return orderStatus;
            }
        }
        return INVALID;
    }
}
