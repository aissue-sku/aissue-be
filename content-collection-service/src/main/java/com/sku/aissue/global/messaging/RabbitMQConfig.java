/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE = "aissue.events";
  public static final String ROUTING_KEY_CONTENT_COLLECTED = "content.collected";
  public static final String ROUTING_KEY_TOPICS_REFRESHED = "topics.refreshed";

  @Bean
  public TopicExchange aissueExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  public Jackson2JsonMessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
    RabbitTemplate template = new RabbitTemplate(cf);
    template.setMessageConverter(converter);
    return template;
  }
}
