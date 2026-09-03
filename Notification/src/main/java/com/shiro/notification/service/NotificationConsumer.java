package com.shiro.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiro.notification.constants.NotificationConstants;
import com.shiro.notification.constants.NotificationType;
import com.shiro.notification.dto.NotificationMessage;
import com.shiro.notification.entity.NotificationTemplate;
import com.shiro.notification.repository.NotificationTemplateRepository;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consumes notification events, resolves templates, and renders notification content. */
@Service
@AllArgsConstructor
public class NotificationConsumer {
  private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
  private static final Pattern PARAMETER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");
  private static final String CACHE_PREFIX = "notification:template:";
  private final ObjectMapper objectMapper;
  private final NotificationTemplateRepository templates;
  private final StringRedisTemplate redis;

  /** Consumes a notification request, resolves its template, and renders the message. */
  @KafkaListener(topics = "${app.kafka.topic}", groupId = "${app.kafka.group-id}")
  @Transactional(readOnly = true)
  public void consume(String payload) throws Exception {
    log.info("KAFKA_CONSUMER_IN consumer={} payload={}", "NotificationConsumer.consume", payload);
    NotificationMessage message = objectMapper.readValue(payload, NotificationMessage.class);
    NotificationType type = NotificationType.valueOf(message.type());
    TemplateData template = template(message.templateCode());
    if (!type.equals(template.type())) {
      throw new IllegalArgumentException(NotificationConstants.TYPE_MISMATCH);
    }
    String content = render(template.content(), message.params());
    log.info(
        "KAFKA_CONSUMER_OUT consumer={} type={} to={} template={} content={}",
        "NotificationConsumer.consume",
        type,
        message.recipient(),
        message.templateCode(),
        content);
  }

  /** Reads a notification template from Redis or loads and caches it from the database. */
  private TemplateData template(String code) {
    String key = CACHE_PREFIX + code;
    Map<Object, Object> cached = redis.opsForHash().entries(key);
    if (!cached.isEmpty()) {
      return new TemplateData(
          String.valueOf(cached.get(NotificationConstants.CACHE_CONTENT)),
          NotificationType.valueOf(String.valueOf(cached.get(NotificationConstants.CACHE_TYPE))));
    }
    NotificationTemplate value = templates.findByCode(code).orElseThrow();
    redis
        .opsForHash()
        .putAll(
            key,
            Map.of(
                NotificationConstants.CACHE_CONTENT,
                value.getContent(),
                NotificationConstants.CACHE_TYPE,
                value.getType().name()));
    redis.expire(key, Duration.ofDays(1));
    return new TemplateData(value.getContent(), value.getType());
  }

  /** Replaces template placeholders with request parameters. */
  private String render(String template, Map<String, Object> params) {
    Matcher matcher = PARAMETER.matcher(template);
    StringBuilder output = new StringBuilder();
    while (matcher.find()) {
      String key = matcher.group(1);
      if (!params.containsKey(key)) {
        throw new IllegalArgumentException(NotificationConstants.MISSING_TEMPLATE_PARAMETER + key);
      }
      matcher.appendReplacement(output, Matcher.quoteReplacement(String.valueOf(params.get(key))));
    }
    matcher.appendTail(output);
    return output.toString();
  }

  private record TemplateData(String content, NotificationType type) {}
}
