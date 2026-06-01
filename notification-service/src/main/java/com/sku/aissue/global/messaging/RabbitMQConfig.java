/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE = "aissue.events";
  public static final String QUEUE_CARDS_GENERATED = "aissue.queue.notification.cards-generated";
  public static final String ROUTING_KEY_CARDS_GENERATED = "cards.generated";

  @Bean
  public TopicExchange aissueExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  public Queue queueCardsGenerated() {
    return new Queue(QUEUE_CARDS_GENERATED, true);
  }

  @Bean
  public Binding bindingCardsGenerated(Queue queueCardsGenerated, TopicExchange aissueExchange) {
    return BindingBuilder.bind(queueCardsGenerated).to(aissueExchange).with(ROUTING_KEY_CARDS_GENERATED);
  }

  @Bean
  public Jackson2JsonMessageConverter jsonMessageConverter() {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    converter.setAlwaysConvertToInferredType(true);
    return converter;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
    RabbitTemplate template = new RabbitTemplate(cf);
    template.setMessageConverter(converter);
    return template;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(cf);
    factory.setMessageConverter(converter);
    return factory;
  }
}