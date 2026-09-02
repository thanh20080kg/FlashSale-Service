package com.shiro.payment.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitConfiguration {
  @Bean
  Queue paymentCommandQueue(
      @org.springframework.beans.factory.annotation.Value("${app.rabbit-queue.payment-command}")
          String queue) {
    return QueueBuilder.durable(queue).build();
  }
}
