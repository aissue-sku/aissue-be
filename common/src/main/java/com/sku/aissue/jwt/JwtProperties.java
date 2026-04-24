/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * JWT 관련 설정 속성
 *
 * <p>환경변수로부터 값을 읽어와서 안전하게 관리
 */
@Component
@Getter
public class JwtProperties {

  /** JWT 시크릿 키 */
  @Value("${spring.jwt.secret}")
  private String secret;

  /** Access Token 유효 기간 (초) */
  @Value("${spring.jwt.access-token-validity-in-seconds:10800}")
  private long accessTokenValidityInSeconds;

  /** Refresh Token 유효 기간 (초) */
  @Value("${spring.jwt.refresh-token-validity-in-seconds:604800}")
  private long refreshTokenValidityInSeconds;
}
