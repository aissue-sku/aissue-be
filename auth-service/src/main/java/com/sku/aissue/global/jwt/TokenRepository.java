/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.jwt;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.sku.aissue.jwt.JwtProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 토큰 관리를 위한 저장소
 *
 * <p>Redis를 사용하여 Refresh Token 및 블랙리스트 토큰을 관리합니다. Refresh Token은 사용자별로 저장되며, 블랙리스트는 로그아웃된 토큰을
 * 관리합니다.
 *
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TokenRepository {

  private final RedisTemplate<String, Object> redisTemplate;
  private final JwtProperties jwtProperties;

  /** Refresh Token Redis 키 접두사 */
  private static final String REFRESH_TOKEN_PREFIX = "RT:";

  /** 블랙리스트 Redis 키 접두사 */
  private static final String BLACKLIST_PREFIX = "BL:";

  /**
   * Refresh Token을 Redis에 저장합니다.
   *
   * <p>사용자 이름을 키로 사용하여 Refresh Token을 저장하며, 설정된 유효 기간 동안 유지됩니다.
   *
   * @param username 사용자 이름
   * @param refreshToken Refresh Token
   */
  public void saveRefreshToken(String username, String refreshToken) {
    String key = REFRESH_TOKEN_PREFIX + username;
    redisTemplate
        .opsForValue()
        .set(key, refreshToken, jwtProperties.getRefreshTokenValidityInSeconds(), TimeUnit.SECONDS);
    log.debug("Refresh Token saved for user: {}", username);
  }

  /**
   * 사용자의 Refresh Token을 조회합니다.
   *
   * @param username 사용자 이름
   * @return Refresh Token 또는 null
   */
  public String findRefreshToken(String username) {
    String key = REFRESH_TOKEN_PREFIX + username;
    Object token = redisTemplate.opsForValue().get(key);
    return token != null ? token.toString() : null;
  }

  /**
   * 사용자의 Refresh Token을 삭제합니다.
   *
   * <p>로그아웃 시 호출되어 저장된 Refresh Token을 제거합니다.
   *
   * @param username 사용자 이름
   */
  public void deleteRefreshToken(String username) {
    String key = REFRESH_TOKEN_PREFIX + username;
    redisTemplate.delete(key);
    log.debug("Refresh Token deleted for user: {}", username);
  }

  /**
   * Access Token을 블랙리스트에 추가합니다.
   *
   * <p>로그아웃된 Access Token을 블랙리스트에 추가하여 더 이상 사용할 수 없게 합니다. 토큰은 원래 만료 시간까지만 블랙리스트에 유지됩니다.
   *
   * @param token Access Token
   * @param expiration 만료 시간(초)
   */
  public void addToBlacklist(String token, long expiration) {
    String key = BLACKLIST_PREFIX + token;
    redisTemplate.opsForValue().set(key, "blacklisted", expiration, TimeUnit.SECONDS);
  }

  /**
   * 토큰이 블랙리스트에 있는지 확인합니다.
   *
   * <p>토큰 검증 시 호출되어 해당 토큰이 로그아웃되었는지 확인합니다.
   *
   * @param token 확인할 토큰
   * @return 블랙리스트에 있으면 true, 없으면 false
   */
  public boolean isBlacklisted(String token) {
    String key = BLACKLIST_PREFIX + token;
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }
}
