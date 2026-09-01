package com.shiro.scheduler.messaging.kafka;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaTriggerProducer {
  private static final Logger log = LoggerFactory.getLogger(KafkaTriggerProducer.class);
  private static final String EMPTY_PAYLOAD = "";

  private final KafkaTemplate<String, String> kafkaTemplate;

  public void send(String topic) {
    kafkaTemplate
        .send(topic, EMPTY_PAYLOAD)
        .whenComplete(
            (result, exception) -> {
              if (exception != null) {
                log.error(
                    "Could not publish scheduler trigger to Kafka topic '{}'", topic, exception);
              } else {
                log.debug("Published scheduler trigger to Kafka topic '{}'", topic);
              }
            });
  }
}
