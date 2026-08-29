package com.shiro.flashsale.service;

import java.util.Map;

public interface NotificationService {
  void sendEmail(String templateCode, String recipient, Map<String, ?> params);

  void sendSms(String templateCode, String recipient, Map<String, ?> params);
}
