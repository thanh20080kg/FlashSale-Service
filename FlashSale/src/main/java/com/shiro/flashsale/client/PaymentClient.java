package com.shiro.flashsale.client;

import com.shiro.flashsale.config.AppProperties;
import com.shiro.flashsale.dto.client.PaymentDtos;
import com.shiro.flashsale.messaging.rabit.RabbitProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentClient {
  private final RabbitProducer producer;
  private final AppProperties properties;

  public String send(PaymentDtos.Request request) throws Exception {
    return producer.send(properties.getPayment().getQueue(), request);
  }
}
