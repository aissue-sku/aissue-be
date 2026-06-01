/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

  public void sendDirect(String userId, String keyword, String title) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set(INTERNAL_TOKEN_HEADER, internalToken);
      restTemplate.exchange(
          "http://notification-service/internal/notifications/direct",
          HttpMethod.POST,
          new HttpEntity<>(new DirectRequest(userId, keyword, title), headers),
          Void.class);
      log.info("포인트 알림 전송 완료 - userId: {}", userId);
    } catch (Exception e) {
      log.error("알림 서비스 호출 실패 - userId: {}, error: {}", userId, e.getMessage());
    }
  }

  private record DirectRequest(String userId, String keyword, String title) {}
}
