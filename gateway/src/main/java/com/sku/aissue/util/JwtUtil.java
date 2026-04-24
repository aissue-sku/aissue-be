/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.util;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/** Gateway 전용 JWT 파싱/검증 유틸 WebFlux 환경에서 사용하며, common 모듈에 의존하지 않음 */
@Slf4j
@Component
public class JwtUtil {

  private static final String TOKEN_TYPE_ACCESS = "access";

  @Value("${spring.jwt.secret}")
  private String secret;

  private SecretKey key;

  @PostConstruct
  public void init() {
    try {
      byte[] keyBytes = Decoders.BASE64.decode(secret);
      this.key = Keys.hmacShaKeyFor(keyBytes);
    } catch (Exception e) {
      log.warn("Base64 디코딩 실패, 일반 텍스트로 처리합니다: {}", e.getMessage());
      this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }
  }

  /** 서명·만료·타입을 한 번의 파싱으로 검증하고 Claims 반환. 유효하지 않으면 null 반환. */
  public Claims validateAndGetClaims(String token) {
    try {
      Claims claims = parseClaims(token);
      if (!TOKEN_TYPE_ACCESS.equals(claims.get("type", String.class))) {
        log.info("잘못된 토큰 타입");
        return null;
      }
      return claims;
    } catch (Exception e) {
      log.info("유효하지 않은 JWT 토큰: {}", e.getMessage());
      return null;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
