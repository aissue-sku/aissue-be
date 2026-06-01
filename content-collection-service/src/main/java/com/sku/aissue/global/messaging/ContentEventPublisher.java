/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.messaging;

import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentEventPublisher {

  private final RabbitTemplate rabbitTemplate;

  public void publishContentCollected(List<Long> contentIds) {
    if (contentIds == null || contentIds.isEmpty()) return;
    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE,
          RabbitMQConfig.ROUTING_KEY_CONTENT_COLLECTED,
          new ContentCollectedEvent(contentIds));
      log.info("content.collected 이벤트 발행 - {}건", contentIds.size());
    } catch (Exception e) {
      log.error("content.collected 이벤트 발행 실패 - {}건: {}", contentIds.size(), e.getMessage());
    }
  }

  public void publishTopicsRefreshed() {
    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE,
          RabbitMQConfig.ROUTING_KEY_TOPICS_REFRESHED,
          new TopicsRefreshedEvent());
      log.info("topics.refreshed 이벤트 발행");
    } catch (Exception e) {
      log.error("topics.refreshed 이벤트 발행 실패: {}", e.getMessage());
    }
  }

  public record ContentCollectedEvent(List<Long> ids) {}

  public record TopicsRefreshedEvent() {}
}