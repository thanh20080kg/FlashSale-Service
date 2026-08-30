package com.shiro.authentication.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiro.authentication.service.NotificationService;
import java.util.Map;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Publishes notification requests; delivery is owned by the Notification service. */
@Service
public class NotificationServiceImpl implements NotificationService {
  private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final String topic;

  public NotificationServiceImpl(
      KafkaTemplate<String, String> kafkaTemplate,
      ObjectMapper objectMapper,
      @Value("${app.kafka.topic:notification.requested}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.topic = topic;
  }

  @Override
  public void sendEmail(String templateCode, String recipient, Map<String, ?> params) {
    publish("EMAIL", templateCode, recipient, params);
  }

  @Override
  public void sendSms(String templateCode, String recipient, Map<String, ?> params) {
    publish("SMS", templateCode, recipient, params);
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
