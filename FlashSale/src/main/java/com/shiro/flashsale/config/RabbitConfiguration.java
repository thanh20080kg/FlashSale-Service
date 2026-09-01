package com.shiro.flashsale.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfiguration {
  @Bean
  Queue warehouseInventoryQueue(AppProperties properties) {
    return QueueBuilder.durable(properties.getWarehouse().getQueue()).build();
  }

  @Bean
  Queue purchaseStatusSyncQueue(AppProperties properties) {
    return QueueBuilder.durable(properties.getWarehouse().getStatusSyncQueue()).build();
  }

  @Bean
  MessageConverter stringMessageConverter() {
    return new SimpleMessageConverter();
  }

  @Bean
  RabbitTemplate rabbitTemplate(
      ConnectionFactory factory, MessageConverter converter, AppProperties properties) {
    RabbitTemplate template = new RabbitTemplate(factory);
    template.setMessageConverter(converter);
    template.setReplyTimeout(properties.getWarehouse().getTimeout().toMillis());
    return template;
  }
}
