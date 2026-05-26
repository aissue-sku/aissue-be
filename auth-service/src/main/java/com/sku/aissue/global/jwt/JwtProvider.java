/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.jwt;

import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.sku.aissue.domain.dto.response.TokenResponse;
import com.sku.aissue.domain.exception.AuthErrorCode;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.jwt.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 토큰 생성 및 검증을 담당하는 클래스
 *
 * <p>이 클래스는 JWT 토큰의 생성, 검증, 파싱 등의 기능을 제공합니다. Access Token과 Refresh Token을 모두 지원하며, Redis를 통한 토큰 관리
 * 기능을 포함합니다.
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtProvider {

  /** JWT 설정 속성 */
  private final JwtProperties jwtProperties;

  /** JWT 서명 키 */
  private SecretKey key;

  /** 토큰 저장소 */
  private final TokenRepository tokenRepository;

  /** Access Token 타입 상수 */
  public static final String TOKEN_TYPE_ACCESS = "access";

  /** Refresh Token 타입 상수 */
  public static final String TOKEN_TYPE_REFRESH = "refresh";

  /** JWT 토큰이 담겨 오는 HTTP 헤더 이름 */
  private static final String AUTHORIZATION_HEADER = "Authorization";

  /** JWT 토큰 접두어 */
  private static final String BEARER_PREFIX = "Bearer ";

  @Value("${app.cookie.secure:false}")
  private boolean cookieSecure;

  public JwtProvider(JwtProperties jwtProperties, TokenRepository tokenRepository) {
    this.jwtProperties = jwtProperties;
    this.tokenRepository = tokenRepository;
  }

  @PostConstruct
  public void init() {
    try {
      byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
      this.key = Keys.hmacShaKeyFor(keyBytes);
    } catch (Exception e) {
      log.warn("Base64 디코딩 실패, 일반 텍스트로 처리합니다: {}", e.getMessage());
      this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }
    log.info("JWT key initialized");
  }

  public TokenResponse createTokens(Authentication authentication) {
    String username = authentication.getName();
    String accessToken = createToken(authentication, TOKEN_TYPE_ACCESS);
    String refreshToken = createToken(authentication, TOKEN_TYPE_REFRESH);

    tokenRepository.saveRefreshToken(username, refreshToken);

    return TokenResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .username(username)
        .build();
  }

  public String createToken(Authentication authentication, String tokenType) {
    String authorities =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

    String subject = authentication.getName();
    long now = System.currentTimeMillis();
    Date validity;

    if (TOKEN_TYPE_REFRESH.equals(tokenType)) {
      validity = new Date(now + jwtProperties.getRefreshTokenValidityInSeconds() * 1000);
    } else {
      validity = new Date(now + jwtProperties.getAccessTokenValidityInSeconds() * 1000);
    }

    return Jwts.builder()
        .subject(subject)
        .claim("auth", authorities)
        .claim("type", tokenType)
        .issuedAt(new Date(now))
        .expiration(validity)
        .signWith(key)
        .compact();
  }

  /** Refresh Token의 클레임을 그대로 사용해 새 Access Token 발급 (SecurityContext 불필요) */
  public String createAccessTokenFromRefreshToken(String refreshToken) {
    Claims claims = parseClaims(refreshToken);
    long now = System.currentTimeMillis();
    Date validity = new Date(now + jwtProperties.getAccessTokenValidityInSeconds() * 1000);

    return Jwts.builder()
        .subject(claims.getSubject())
        .claim("auth", claims.get("auth", String.class))
        .claim("type", TOKEN_TYPE_ACCESS)
        .issuedAt(new Date(now))
        .expiration(validity)
        .signWith(key)
        .compact();
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public String getUsernameFromToken(String token) {
    return parseClaims(token).getSubject();
  }

  private String getTokenType(String token) {
    return parseClaims(token).get("type", String.class);
  }

  public boolean validateToken(String token) {
    try {
      // 서명·만료 검증 먼저 → 위조 토큰에 Redis 쿼리 낭비 방지
      parseClaims(token);

      if (tokenRepository.isBlacklisted(token)) {
        log.info("블랙리스트에 등록된 토큰입니다.");
        return false;
      }
      return true;
    } catch (SignatureException | MalformedJwtException e) {
      log.info("잘못된 JWT 서명입니다.");
    } catch (ExpiredJwtException e) {
      log.info("만료된 JWT 토큰입니다.");
    } catch (UnsupportedJwtException e) {
      log.info("지원되지 않는 JWT 토큰입니다.");
    } catch (IllegalArgumentException e) {
      log.info("JWT 토큰이 잘못되었습니다.");
    }
    return false;
  }

  /** Access Token 전용 통합 검증 — 서명·만료·블랙리스트·타입을 한 번의 파싱으로 처리. 유효하면 Claims 반환, 아니면 null. */
  public Claims validateAndGetAccessClaims(String token) {
    try {
      Claims claims = parseClaims(token);
      if (tokenRepository.isBlacklisted(token)) {
        log.info("블랙리스트에 등록된 토큰입니다.");
        return null;
      }
      if (!TOKEN_TYPE_ACCESS.equals(claims.get("type", String.class))) {
        return null;
      }
      return claims;
    } catch (SignatureException | MalformedJwtException e) {
      log.info("잘못된 JWT 서명입니다.");
    } catch (ExpiredJwtException e) {
      log.info("만료된 JWT 토큰입니다.");
    } catch (UnsupportedJwtException e) {
      log.info("지원되지 않는 JWT 토큰입니다.");
    } catch (IllegalArgumentException e) {
      log.info("JWT 토큰이 잘못되었습니다.");
    }
    return null;
  }

  public boolean validateTokenType(String token, String expectedType) {
    try {
      return expectedType.equals(getTokenType(token));
    } catch (Exception e) {
      throw new CustomException(AuthErrorCode.JWT_TOKEN_EXPIRED);
    }
  }

  public long getExpirationTime(String token) {
    try {
      Date expiration = parseClaims(token).getExpiration();
      return (expiration.getTime() - System.currentTimeMillis()) / 1000;
    } catch (Exception e) {
      return 0;
    }
  }

  public void blacklistToken(String token) {
    long expiration = getExpirationTime(token);
    if (expiration > 0) {
      tokenRepository.addToBlacklist(token, expiration);
    }
  }

  public boolean validateRefreshToken(String username, String refreshToken) {
    String storedToken = tokenRepository.findRefreshToken(username);
    return storedToken != null && storedToken.equals(refreshToken);
  }

  public void deleteRefreshToken(String username) {
    tokenRepository.deleteRefreshToken(username);
  }

  public void addJwtToCookie(HttpServletResponse response, String token, String name, long maxAge) {
    Cookie cookie = new Cookie(name, token);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure);
    cookie.setPath("/");
    cookie.setMaxAge((int) maxAge);
    response.addCookie(cookie);

    log.info("JWT 쿠키가 설정되었습니다 - 이름: {}, 만료: {}초", name, cookie.getMaxAge());
  }

  public void removeJwtCookie(HttpServletResponse response, String name) {
    Cookie cookie = new Cookie(name, null);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecure);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);

    log.info("JWT 쿠키가 삭제되었습니다 - 이름: {}", name);
  }

  public String extractAccessToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
    if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    } else if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("ACCESS_TOKEN".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  public String extractRefreshToken(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("REFRESH_TOKEN".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }

    throw new CustomException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
  }
}
