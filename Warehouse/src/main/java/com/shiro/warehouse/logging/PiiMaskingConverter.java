package com.shiro.warehouse.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ObjectUtils;

public class PiiMaskingConverter extends CompositeConverter<ILoggingEvent> {
  private static final String SEPARATOR = "|||";
  private static final String KEEP_EMAIL = "KEEP_EMAIL";
  private static final String KEEP_LAST_3 = "KEEP_LAST_3";
  private final List<MaskRule> rules = new ArrayList<>();

  @Override
  public void start() {
    getContext().getCopyOfPropertyMap().entrySet().stream()
        .filter(entry -> entry.getKey().startsWith("MASK_"))
        .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
        .forEach(
            entry -> {
              String propertyName = entry.getKey();
              String rule = getContext().getProperty(propertyName);
              if (ObjectUtils.isEmpty(rule)) {
                return;
              }
              String[] parts = rule.split("\\Q" + SEPARATOR + "\\E", 2);
              if (parts.length != 2) {
                addError("Invalid masking rule " + propertyName + ". Expected regex|||replacement");
                return;
              }
              try {
                rules.add(new MaskRule(Pattern.compile(parts[0]), parts[1]));
              } catch (RuntimeException exception) {
                addError("Invalid regex in " + propertyName + ": " + exception.getMessage());
              }
            });
    super.start();
  }

  @Override
  protected String transform(ILoggingEvent event, String message) {
    if (ObjectUtils.isEmpty(message)) {
      return message;
    }
    String masked = message;
    for (MaskRule rule : rules) {
      Matcher matcher = rule.pattern().matcher(masked);
      StringBuilder result = new StringBuilder();
      while (matcher.find()) {
        String configured = rule.replacement();
        if (KEEP_EMAIL.equals(configured) || KEEP_LAST_3.equals(configured)) {
          matcher.appendReplacement(
              result, Matcher.quoteReplacement(replacement(configured, matcher)));
        } else {
          matcher.appendReplacement(result, configured);
        }
      }
      matcher.appendTail(result);
      masked = result.toString();
    }
    return masked;
  }

  private String replacement(String configured, Matcher matcher) {
    String matched = matcher.group();
    if (KEEP_EMAIL.equals(configured)) {
      return matcher.group(1) + "*".repeat(matcher.group(2).length()) + matcher.group(3);
    }
    if (KEEP_LAST_3.equals(configured)) {
      return "*".repeat(Math.max(0, matched.length() - 3))
          + matched.substring(Math.max(0, matched.length() - 3));
    }
    return configured;
  }

  private record MaskRule(Pattern pattern, String replacement) {}
}
