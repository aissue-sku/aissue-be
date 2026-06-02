/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.messaging;

import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.sku.aissue.domain.dto.request.NotificationMatchRequest;
import com.sku.aissue.domain.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardsGeneratedEventListener {

  private final NotificationService notificationService;

  @RabbitListener(queues = RabbitMQConfig.QUEUE_CARDS_GENERATED)
  public void onCardsGenerated(CardsGeneratedEvent event) {
    if (event == null || event.cards() == null || event.cards().isEmpty()) return;
    log.info("cards.generated 이벤트 수신 - {}개 카드", event.cards().size());
    try {
      NotificationMatchRequest request = toMatchRequest(event.cards());
      notificationService.matchAndNotify(request);
    } catch (Exception e) {
      log.error("알림 매칭 처리 실패: {}", e.getMessage());
    }
  }

  private NotificationMatchRequest toMatchRequest(List<CardInfo> cards) {
    List<NotificationMatchRequest.CardInfo> cardInfos =
        cards.stream()
            .map(
                c -> {
                  NotificationMatchRequest.CardInfo info = new NotificationMatchRequest.CardInfo();
                  info.setTitle(c.title());
                  info.setUrl(c.url());
                  info.setContentId(c.contentId());
                  info.setKeywords(c.keywords());
                  return info;
                })
            .toList();
    NotificationMatchRequest request = new NotificationMatchRequest();
    request.setCards(cardInfos);
    return request;
  }

  public record CardInfo(String title, String url, Long contentId, List<String> keywords) {}

  public record CardsGeneratedEvent(List<CardInfo> cards) {}
}
