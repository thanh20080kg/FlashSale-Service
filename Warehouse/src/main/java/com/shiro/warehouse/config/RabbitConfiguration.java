package com.shiro.warehouse.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfiguration {

  @Bean
  Queue inventoryQueue(@Value("${app.rabbit-queue.inventory-queue}") String queue) {
    return QueueBuilder.durable(queue).build();
  }

  @Bean
  Queue purchaseStatusSyncQueue(@Value("${app.rabbit-queue.status-sync-queue}") String queue) {
    return QueueBuilder.durable(queue).build();
  }

  @Bean
  MessageConverter stringMessageConverter() {
    return new SimpleMessageConverter();
  }

  @Bean
  SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory,
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      MessageConverter messageConverter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setMessageConverter(messageConverter);
    return factory;
  }

  @Bean
  RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(converter);
    template.setReplyTimeout(5000);
    return template;
  }
}
