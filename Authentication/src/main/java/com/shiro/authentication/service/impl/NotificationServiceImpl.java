package com.shiro.authentication.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiro.authentication.constants.NotificationChannel;
import com.shiro.authentication.service.NotificationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
  private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.kafka.topic.notification}")
  private String topic;

  @Override
  public void sendEmail(String templateCode, String recipient, Map<String, ?> params) {
    publish(NotificationChannel.EMAIL.name(), templateCode, recipient, params);
  }

  @Override
  public void sendSms(String templateCode, String recipient, Map<String, ?> params) {
    publish(NotificationChannel.SMS.name(), templateCode, recipient, params);
  }

  private void publish(String type, String templateCode, String recipient, Map<String, ?> params) {
    try {
      String payload =
          objectMapper.writeValueAsString(
              new NotificationMessage(type, templateCode, recipient, params));
      kafkaTemplate
          .send(topic, payload)
          .whenComplete(
              (result, exception) -> {
                if (ObjectUtils.isNotEmpty(exception)) {
                  log.error("Could not publish notification to Kafka topic '{}'", topic, exception);
                }
              });
    } catch (RuntimeException exception) {
      throw new IllegalStateException(
          "Could not publish notification to Kafka topic '" + topic + "'", exception);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize notification request", exception);
    }
  }

  private record NotificationMessage(
      String type, String templateCode, String recipient, Map<String, ?> params) {}
}
