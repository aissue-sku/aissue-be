/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.sku.aissue.domain.dto.request.LoginRequest;
import com.sku.aissue.domain.dto.response.TokenResponse;
import com.sku.aissue.domain.exception.AuthErrorCode;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.global.jwt.JwtProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtProvider jwtProvider;

  @Override
  public TokenResponse login(LoginRequest loginRequest) {
    UsernamePasswordAuthenticationToken authenticationToken =
        new UsernamePasswordAuthenticationToken(
            loginRequest.getUsername(), loginRequest.getPassword());

    Authentication authenticated;
    try {
      authenticated = authenticationManager.authenticate(authenticationToken);
    } catch (BadCredentialsException e) {
      log.warn("로그인 실패(자격 증명 불일치): username={}", loginRequest.getUsername());
      throw new CustomException(AuthErrorCode.LOGIN_FAIL);
    } catch (AuthenticationException e) {
      log.warn("로그인 실패(인증 오류): username={}", loginRequest.getUsername());
      throw new CustomException(AuthErrorCode.LOGIN_FAIL);
    }

    TokenResponse tokenResponse = jwtProvider.createTokens(authenticated);
    log.info("로그인 성공: {}", loginRequest.getUsername());
    return tokenResponse;
  }

  @Override
  public String logout(String accessToken) {
    String username = jwtProvider.getUsernameFromToken(accessToken);

    jwtProvider.deleteRefreshToken(username);
    jwtProvider.blacklistToken(accessToken);

    log.info("로그아웃 성공: {}", username);
    return "로그아웃 성공 - 사용자: " + username;
  }

  @Override
  public String reissueAccessToken(String refreshToken) {
    String username = jwtProvider.getUsernameFromToken(refreshToken);

    if (!jwtProvider.validateRefreshToken(username, refreshToken)) {
      throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    log.info("AT 재발급 성공: {}", username);
    return jwtProvider.createAccessTokenFromRefreshToken(refreshToken);
  }
}
