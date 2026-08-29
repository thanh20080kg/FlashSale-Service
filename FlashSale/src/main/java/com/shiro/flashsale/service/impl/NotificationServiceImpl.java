package com.shiro.flashsale.service.impl;

import com.shiro.flashsale.constants.NotificationType;
import com.shiro.flashsale.entity.NotificationTemplate;
import com.shiro.flashsale.exception.ApiException;
import com.shiro.flashsale.exception.ErrorCode;
import com.shiro.flashsale.repository.NotificationTemplateRepository;
import com.shiro.flashsale.service.NotificationService;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {
  private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
  private static final Pattern PARAMETER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");
  private static final Duration TEMPLATE_CACHE_TTL = Duration.ofDays(1);
  private static final String TEMPLATE_CACHE_PREFIX = "notification:template:";
  private final NotificationTemplateRepository templates;
  private final StringRedisTemplate redis;

  public NotificationServiceImpl(
      NotificationTemplateRepository templates, StringRedisTemplate redis) {
    this.templates = templates;
    this.redis = redis;
  }

  @Override
  @Transactional(readOnly = true)
  public void sendEmail(String templateCode, String recipient, Map<String, ?> params) {
    send(NotificationType.EMAIL, templateCode, recipient, params);
  }

  @Override
  @Transactional(readOnly = true)
  public void sendSms(String templateCode, String recipient, Map<String, ?> params) {
    send(NotificationType.SMS, templateCode, recipient, params);
  }

  private void send(
      NotificationType expectedType, String code, String recipient, Map<String, ?> params) {
    TemplateData template = getTemplate(code);
    if (template.type() != expectedType)
      throw ApiException.of(ErrorCode.NOTIFICATION_TEMPLATE_TYPE_MISMATCH);
    String content = render(template.content(), params);
    if (expectedType == NotificationType.EMAIL)
      log.info("MOCK EMAIL to={} template={} content={}", recipient, code, content);
    else log.info("MOCK SMS to={} template={} content={}", recipient, code, content);
  }

  private TemplateData getTemplate(String code) {
    String key = TEMPLATE_CACHE_PREFIX + code;
    Map<Object, Object> cached = redis.opsForHash().entries(key);
    if (!cached.isEmpty()) {
      return new TemplateData(
          String.valueOf(cached.get("content")),
          NotificationType.valueOf(String.valueOf(cached.get("type"))));
    }
    NotificationTemplate template =
        templates
            .findByCode(code)
            .orElseThrow(
                () ->
                    new ApiException(
                        ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND,
                        "Notification template not found: " + code));
    redis
        .opsForHash()
        .putAll(key, Map.of("content", template.getContent(), "type", template.getType().name()));
    redis.expire(key, TEMPLATE_CACHE_TTL);
    return new TemplateData(template.getContent(), template.getType());
  }

  private String render(String template, Map<String, ?> params) {
    Matcher matcher = PARAMETER.matcher(template);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String key = matcher.group(1);
      if (!params.containsKey(key))
        throw new ApiException(
            ErrorCode.NOTIFICATION_PARAMETER_MISSING, "Missing template parameter: " + key);
      matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(params.get(key))));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private record TemplateData(String content, NotificationType type) {}
}
