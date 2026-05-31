/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import java.util.Collections;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
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

  public void sendDirect(String userId, String title, String url, Long contentId) {
    if (userId == null) return;
    try {
      restTemplate.postForObject(
          "http://notification-service/internal/notifications/direct",
          new DirectRequest(userId, "직접 분석", title, url, contentId),
          Void.class);
      log.info("직접 분석 알림 전송 - userId: {}", userId);
    } catch (Exception e) {
      log.error("알림 서비스 호출 실패 (direct) - error: {}", e.getMessage());
    }
  }

  public List<String> getUserSubscribedKeywords(String userId) {
    try {
      var response =
          restTemplate.exchange(
              "http://notification-service/internal/notifications/subscriptions/" + userId,
              HttpMethod.GET,
              null,
              new ParameterizedTypeReference<List<String>>() {});
      List<String> body = response.getBody();
      return body != null ? body : Collections.emptyList();
    } catch (Exception e) {
      log.warn("사용자 구독 키워드 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
      return Collections.emptyList();
    }
  }

  public List<PopularKeywordInfo> getPopularKeywords() {
    try {
      var response =
          restTemplate.exchange(
              "http://notification-service/internal/notifications/popular-keywords",
              HttpMethod.GET,
              null,
              new ParameterizedTypeReference<List<PopularKeywordInfo>>() {});
      List<PopularKeywordInfo> body = response.getBody();
      return body != null ? body : Collections.emptyList();
    } catch (Exception e) {
      log.error("인기 키워드 조회 실패 - error: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  public record PopularKeywordInfo(String keyword, long subscriberCount) {}

  public record CardInfo(String title, String url, Long contentId, List<String> keywords) {}

  private record MatchRequest(List<CardInfo> cards) {}

  private record DirectRequest(
      String userId, String keyword, String title, String url, Long contentId) {}
}
