/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationServiceClient {

  private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

  private final RestTemplate restTemplate;

  @Value("${internal.token}")
  private String internalToken;

  private HttpEntity<Void> internalRequest() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(INTERNAL_TOKEN_HEADER, internalToken);
    return new HttpEntity<>(headers);
  }

  private <T> HttpEntity<T> internalRequest(T body) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(INTERNAL_TOKEN_HEADER, internalToken);
    return new HttpEntity<>(body, headers);
  }

  @CircuitBreaker(name = "notification", fallbackMethod = "matchAndNotifyFallback")
  public void matchAndNotify(List<CardInfo> cards) {
    if (cards == null || cards.isEmpty()) return;
    restTemplate.exchange(
        "http://notification-service/internal/notifications/match",
        HttpMethod.POST,
        internalRequest(new MatchRequest(cards)),
        Void.class);
    log.info("알림 매칭 요청 완료 - {}개 카드", cards.size());
  }

  private void matchAndNotifyFallback(List<CardInfo> cards, Exception e) {
    log.warn("notification-service 서킷 오픈 - matchAndNotify 스킵: {}", e.getMessage());
  }

  @CircuitBreaker(name = "notification", fallbackMethod = "sendDirectFallback")
  public void sendDirect(String userId, String title, String url, Long contentId) {
    if (userId == null) return;
    restTemplate.exchange(
        "http://notification-service/internal/notifications/direct",
        HttpMethod.POST,
        internalRequest(new DirectRequest(userId, "직접 분석", title, url, contentId)),
        Void.class);
    log.info("직접 분석 알림 전송 - userId: {}", userId);
  }

  private void sendDirectFallback(
      String userId, String title, String url, Long contentId, Exception e) {
    log.warn("notification-service 서킷 오픈 - sendDirect 스킵 userId: {}: {}", userId, e.getMessage());
  }

  @CircuitBreaker(name = "notification", fallbackMethod = "getUserSubscribedKeywordsFallback")
  public List<String> getUserSubscribedKeywords(String userId) {
    var response =
        restTemplate.exchange(
            "http://notification-service/internal/notifications/subscriptions/" + userId,
            HttpMethod.GET,
            internalRequest(),
            new ParameterizedTypeReference<List<String>>() {});
    List<String> body = response.getBody();
    return body != null ? body : Collections.emptyList();
  }

  private List<String> getUserSubscribedKeywordsFallback(String userId, Exception e) {
    log.warn("notification-service 서킷 오픈 - getUserSubscribedKeywords 빈 목록 반환 userId: {}", userId);
    return Collections.emptyList();
  }

  @CircuitBreaker(name = "notification", fallbackMethod = "getAllSubscribedUserIdsFallback")
  public List<String> getAllSubscribedUserIds() {
    var response =
        restTemplate.exchange(
            "http://notification-service/internal/notifications/subscribed-user-ids",
            HttpMethod.GET,
            internalRequest(),
            new ParameterizedTypeReference<List<String>>() {});
    List<String> body = response.getBody();
    return body != null ? body : Collections.emptyList();
  }

  private List<String> getAllSubscribedUserIdsFallback(Exception e) {
    log.warn("notification-service 서킷 오픈 - getAllSubscribedUserIds 빈 목록 반환: {}", e.getMessage());
    return Collections.emptyList();
  }

  @CircuitBreaker(name = "notification", fallbackMethod = "getPopularKeywordsFallback")
  public List<PopularKeywordInfo> getPopularKeywords() {
    var response =
        restTemplate.exchange(
            "http://notification-service/internal/notifications/popular-keywords",
            HttpMethod.GET,
            internalRequest(),
            new ParameterizedTypeReference<List<PopularKeywordInfo>>() {});
    List<PopularKeywordInfo> body = response.getBody();
    return body != null ? body : Collections.emptyList();
  }

  private List<PopularKeywordInfo> getPopularKeywordsFallback(Exception e) {
    log.warn("notification-service 서킷 오픈 - getPopularKeywords 빈 목록 반환: {}", e.getMessage());
    return Collections.emptyList();
  }

  public record PopularKeywordInfo(String keyword, long subscriberCount) {}

  public record CardInfo(String title, String url, Long contentId, List<String> keywords) {}

  private record MatchRequest(List<CardInfo> cards) {}

  private record DirectRequest(
      String userId, String keyword, String title, String url, Long contentId) {}
}
