/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/** Gateway 전역 에러 핸들러 WebFlux 환경에서 발생하는 모든 예외를 공통 JSON 형식으로 반환 */
@Slf4j
@Order(-2)
@Component
@RequiredArgsConstructor
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

  private final ObjectMapper objectMapper;

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

    ServerHttpResponse response = exchange.getResponse();

    if (response.isCommitted()) {
      return Mono.error(ex);
    }

    HttpStatus status;
    String message;

    if (ex instanceof ResponseStatusException rse) {
      status = HttpStatus.valueOf(rse.getStatusCode().value());
      message = rse.getReason() != null ? rse.getReason() : "요청을 처리할 수 없습니다.";
    } else {
      log.error("처리되지 않은 예외 발생: {}", ex.getMessage(), ex);
      status = HttpStatus.INTERNAL_SERVER_ERROR;
      message = "서버 내부 오류가 발생했습니다.";
    }

    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("code", status.value());
    body.put("message", message);
    body.put("data", null);

    try {
      byte[] bytes = objectMapper.writeValueAsBytes(body);
      DataBuffer buffer = response.bufferFactory().wrap(bytes);
      return response.writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
      log.error("에러 응답 직렬화 실패: {}", e.getMessage());
      return Mono.error(e);
    }
  }
}
