package com.shiro.flashsale.dto.client;

import java.util.UUID;

public record PaymentResult(
    boolean success, UUID purchaseId, UUID transactionId, String status, String message) {}
