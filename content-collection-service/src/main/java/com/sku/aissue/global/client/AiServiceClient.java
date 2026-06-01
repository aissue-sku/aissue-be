/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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
public class AiServiceClient {

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

  @CircuitBreaker(name = "ai", fallbackMethod = "triggerIssueCardsFallback")
  public void triggerIssueCards() {
    restTemplate.exchange(
        "http://ai-service/internal/ai/issue-cards",
        HttpMethod.POST,
        internalRequest(),
        Void.class);
    log.info("이슈 카드 생성 트리거 완료");
  }

  private void triggerIssueCardsFallback(Exception e) {
    log.warn("ai-service 서킷 오픈 - triggerIssueCards 스킵: {}", e.getMessage());
  }

  @CircuitBreaker(name = "ai", fallbackMethod = "embedFallback")
  public void embed(List<Long> contentIds) {
    if (contentIds == null || contentIds.isEmpty()) return;
    restTemplate.exchange(
        "http://ai-service/internal/ai/embed",
        HttpMethod.POST,
        internalRequest(contentIds),
        Void.class);
    log.info("임베딩 트리거 완료 - {}건", contentIds.size());
  }

  private void embedFallback(List<Long> contentIds, Exception e) {
    log.warn("ai-service 서킷 오픈 - embed 스킵 {}건: {}", contentIds.size(), e.getMessage());
  }
}
