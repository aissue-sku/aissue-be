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
public class CardEventPublisher {

  private final RabbitTemplate rabbitTemplate;

  public void publishCardsGenerated(List<CardInfo> cards) {
    if (cards == null || cards.isEmpty()) return;
    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE,
          RabbitMQConfig.ROUTING_KEY_CARDS_GENERATED,
          new CardsGeneratedEvent(cards));
      log.info("cards.generated 이벤트 발행 - {}개 카드", cards.size());
    } catch (Exception e) {
      log.error("cards.generated 이벤트 발행 실패 - {}개: {}", cards.size(), e.getMessage());
    }
  }

  public record CardInfo(String title, String url, Long contentId, List<String> keywords) {}

  public record CardsGeneratedEvent(List<CardInfo> cards) {}
}
