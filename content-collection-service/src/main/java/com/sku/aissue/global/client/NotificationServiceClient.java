/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationServiceClient {

  private final RestTemplate restTemplate;

  public void matchAndNotify(List<CardInfo> cards) {
    if (cards == null || cards.isEmpty()) return;
    try {
      restTemplate.postForObject(
          "http://notification-service/internal/notifications/match",
          new MatchRequest(cards),
          Void.class);
      log.info("알림 매칭 요청 완료 - {}개 카드", cards.size());
    } catch (Exception e) {
      log.error("알림 서비스 호출 실패 - error: {}", e.getMessage());
    }
  }

  public record CardInfo(String title, String url, Long contentId, List<String> keywords) {}

  private record MatchRequest(List<CardInfo> cards) {}
}
