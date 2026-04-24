/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.filter;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.sku.aissue.util.JwtUtil;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/** JWT 검증 글로벌 필터 검증 완료 후 X-User-Name, X-User-Roles 헤더를 추가하여 하위 서비스에 전달 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

  private final JwtUtil jwtUtil;

  /** 인증 없이 통과할 경로 */
  private static final List<String> PERMIT_PATHS =
      List.of("/api/auths/login", "/api/auths/refresh", "/api/users/sign-up", "/actuator/health");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getURI().getPath();

    if (isPermitPath(path) || HttpMethod.OPTIONS.equals(request.getMethod())) {
      return chain.filter(exchange);
    }

    String token = extractToken(request);

    Claims claims = token != null ? jwtUtil.validateAndGetClaims(token) : null;
    if (claims == null) {
      log.warn("인증 실패: {} {}", request.getMethod(), path);
      return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."));
    }

    // 검증 완료 → 클라이언트 헤더 제거 후 사용자 정보 주입 (헤더 인젝션 방어)
    String username = claims.getSubject();
    String authorities = claims.get("auth", String.class);

    ServerHttpRequest modifiedRequest =
        request
            .mutate()
            .headers(
                h -> {
                  h.remove("X-User-Name");
                  h.remove("X-User-Roles");
                })
            .header("X-User-Name", username)
            .header("X-User-Roles", authorities != null ? authorities : "")
            .build();

    log.debug("인증 성공: user={}, path={}", username, path);
    return chain.filter(exchange.mutate().request(modifiedRequest).build());
  }

  private boolean isPermitPath(String path) {
    return PERMIT_PATHS.stream().anyMatch(path::startsWith);
  }

  private String extractToken(ServerHttpRequest request) {
    // 1. Authorization 헤더
    String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    // 2. 쿠키
    var cookie = request.getCookies().getFirst("ACCESS_TOKEN");
    if (cookie != null) {
      return cookie.getValue();
    }
    return null;
  }

  @Override
  public int getOrder() {
    return -1; // 가장 높은 우선순위
  }
}
