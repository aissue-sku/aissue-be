/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationServiceClient {

  private final RestTemplate restTemplate;

  public void sendDirect(String userId, String keyword, String title) {
    try {
      restTemplate.postForObject(
          "http://notification-service/internal/notifications/direct",
          new DirectRequest(userId, keyword, title),
          Void.class);
      log.info("포인트 알림 전송 완료 - userId: {}", userId);
    } catch (Exception e) {
      log.error("알림 서비스 호출 실패 - userId: {}, error: {}", userId, e.getMessage());
    }
  }

  private record DirectRequest(String userId, String keyword, String title) {}
}
