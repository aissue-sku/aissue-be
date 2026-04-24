/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.request.LoginRequest;
import com.sku.aissue.domain.dto.response.TokenResponse;
import com.sku.aissue.domain.exception.AuthErrorCode;
import com.sku.aissue.domain.service.AuthService;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.global.jwt.JwtProvider;
import com.sku.aissue.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthControllerImpl implements AuthController {

  private final AuthService authService;
  private final JwtProvider jwtProvider;

  @Override
  public ResponseEntity<BaseResponse<String>> login(
      HttpServletResponse response, LoginRequest loginRequest) {

    TokenResponse tokenResponse = authService.login(loginRequest);

    jwtProvider.addJwtToCookie(
        response,
        tokenResponse.getRefreshToken(),
        "REFRESH_TOKEN",
        jwtProvider.getExpirationTime(tokenResponse.getRefreshToken()));

    response.setHeader("Authorization", "Bearer " + tokenResponse.getAccessToken());

    // AT는 Authorization 헤더로 전달, body에는 사용자 아이디만 반환
    return ResponseEntity.status(HttpStatus.OK)
        .body(BaseResponse.success(200, "로그인이 완료되었습니다.", tokenResponse.getUsername()));
  }

  @Override
  public ResponseEntity<BaseResponse<String>> logout(
      HttpServletRequest request, HttpServletResponse response) {

    String accessToken = jwtProvider.extractAccessToken(request);
    if (accessToken == null) {
      throw new CustomException(AuthErrorCode.ACCESS_TOKEN_NOT_FOUND);
    }

    String result = authService.logout(accessToken);

    jwtProvider.removeJwtCookie(response, "REFRESH_TOKEN");

    return ResponseEntity.status(HttpStatus.OK)
        .body(BaseResponse.success(200, "로그아웃이 완료되었습니다.", result));
  }

  @Override
  public ResponseEntity<BaseResponse<String>> reissueAccessToken(
      HttpServletRequest request, HttpServletResponse response) {

    String refreshToken = jwtProvider.extractRefreshToken(request);

    // 서명·만료 검증 먼저, 이후 타입 확인
    if (!jwtProvider.validateToken(refreshToken)) {
      throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }
    if (!jwtProvider.validateTokenType(refreshToken, JwtProvider.TOKEN_TYPE_REFRESH)) {
      throw new CustomException(AuthErrorCode.INVALID_TOKEN_TYPE);
    }

    String newAccessToken = authService.reissueAccessToken(refreshToken);

    response.setHeader("Authorization", "Bearer " + newAccessToken);

    return ResponseEntity.status(HttpStatus.OK)
        .body(BaseResponse.success(200, "액세스 토큰 재발급이 완료되었습니다.", null));
  }
}
