/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import java.util.Map;

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
public class UserServiceClient {

  private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

  private final RestTemplate restTemplate;

  @Value("${internal.token}")
  private String internalToken;

  private <T> HttpEntity<T> internalRequest(T body) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(INTERNAL_TOKEN_HEADER, internalToken);
    return new HttpEntity<>(body, headers);
  }

  public void addPoints(String username, int points, String reason) {
    try {
      restTemplate.exchange(
          "http://user-service/internal/users/by-username/" + username + "/points",
          HttpMethod.POST,
          internalRequest(Map.of("points", points, "reason", reason)),
          Void.class);
      log.info("포인트 지급 완료 - username: {}, points: {}", username, points);
    } catch (Exception e) {
      log.error("포인트 지급 실패 - username: {}, error: {}", username, e.getMessage());
    }
  }
}
