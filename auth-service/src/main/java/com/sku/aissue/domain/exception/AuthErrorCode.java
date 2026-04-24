/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.exception;

import org.springframework.http.HttpStatus;

import com.sku.aissue.exception.model.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
  LOGIN_FAIL("AUTH4001", "로그인 처리 중 오류가 발생하였습니다.", HttpStatus.UNAUTHORIZED),
  TOKEN_FAIL("AUTH4002", "액세스 토큰 요청에 실패하였습니다.", HttpStatus.UNAUTHORIZED),
  USER_INFO_FAIL("AUTH4003", "사용자 정보 요청에 실패하였습니다.", HttpStatus.UNAUTHORIZED),
  INVALID_ACCESS_TOKEN("AUTH4004", "유효하지 않은 액세스 토큰입니다.", HttpStatus.UNAUTHORIZED),
  INVALID_REFRESH_TOKEN("AUTH4005", "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
  ACCESS_TOKEN_EXPIRED("AUTH4006", "액세스 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
  REFRESH_TOKEN_REQUIRED("AUTH4007", "리프레시 토큰이 필요합니다.", HttpStatus.UNAUTHORIZED),
  INVALID_AUTH_CONTEXT("AUTH4008", "SecurityContext에 인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED),
  AUTHENTICATION_NOT_FOUND("AUTH4009", "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
  INVALID_PASSWORD("AUTH4010", "비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
  USERNAME_NOT_FOUND("AUTH4011", "존재하지 않는 아이디입니다.", HttpStatus.NOT_FOUND),

  JWT_TOKEN_EXPIRED("JWT4001", "JWT 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
  UNSUPPORTED_TOKEN("JWT4002", "지원되지 않는 JWT 형식입니다.", HttpStatus.UNAUTHORIZED),
  MALFORMED_JWT_TOKEN("JWT4003", "JWT 형식이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
  INVALID_SIGNATURE("JWT4004", "JWT 서명이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
  ILLEGAL_ARGUMENT("JWT4005", "JWT 토큰 값이 잘못되었습니다.", HttpStatus.UNAUTHORIZED),
  ACCESS_TOKEN_NOT_FOUND("JWT4006", "액세스 토큰을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED),
  REFRESH_TOKEN_NOT_FOUND("JWT4007", "리프레시 토큰을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED),
  INVALID_TOKEN_TYPE("JWT4008", "토큰 타입이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
  ;

  private final String code;
  private final String message;
  private final HttpStatus status;
}
