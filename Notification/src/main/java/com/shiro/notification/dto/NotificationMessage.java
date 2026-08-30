package com.shiro.notification.dto;

import java.util.Map;

public record NotificationMessage(
    String type, String templateCode, String recipient, Map<String, Object> params) {}
