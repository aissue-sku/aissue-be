/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.request.LoginRequest;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "인증", description = "인증 관련 API")
@RequestMapping("/api/auths")
public interface AuthController {

  @Operation(
      summary = "로그인",
      description = "로그인을 수행하여 사용자 아이디를 반환합니다. AT는 Authorization 헤더, RT는 HttpOnly 쿠키로 응답합니다.")
  @SecurityRequirements
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "로그인 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "입력값 유효성 오류",
        content = @Content(schema = @Schema(implementation = BaseResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "아이디 또는 비밀번호 불일치",
        content = @Content(schema = @Schema(implementation = BaseResponse.class)))
  })
  @PostMapping("/login")
  ResponseEntity<BaseResponse<String>> login(
      HttpServletResponse response,
      @Parameter(description = "로그인 정보") @RequestBody @Valid LoginRequest loginRequest);

  @Operation(summary = "로그아웃", description = "로그아웃을 수행하여 사용자의 Redis RT 삭제 + AT 블랙리스트를 처리합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
    @ApiResponse(
        responseCode = "401",
        description = "액세스 토큰 없음 또는 만료",
        content = @Content(schema = @Schema(implementation = BaseResponse.class)))
  })
  @Parameters({
    @Parameter(
        name = "Authorization",
        in = ParameterIn.HEADER,
        description = "Bearer {accessToken}",
        required = true),
    @Parameter(
        name = "REFRESH_TOKEN",
        in = ParameterIn.COOKIE,
        description = "Refresh Token (HttpOnly 쿠키)")
  })
  @PostMapping("/logout")
  ResponseEntity<BaseResponse<String>> logout(
      HttpServletRequest request, HttpServletResponse response);

  @Operation(
      summary = "액세스 토큰 재발급",
      description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다. 새 AT는 Authorization 헤더로 응답합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "액세스 토큰 재발급 성공"),
    @ApiResponse(
        responseCode = "401",
        description = "Refresh Token 없음, 만료 또는 타입 불일치",
        content = @Content(schema = @Schema(implementation = BaseResponse.class)))
  })
  @Parameters({
    @Parameter(
        name = "REFRESH_TOKEN",
        in = ParameterIn.COOKIE,
        description = "Refresh Token (HttpOnly 쿠키)",
        required = true)
  })
  @PostMapping("/refresh")
  ResponseEntity<BaseResponse<String>> reissueAccessToken(
      HttpServletRequest request, HttpServletResponse response);
}
