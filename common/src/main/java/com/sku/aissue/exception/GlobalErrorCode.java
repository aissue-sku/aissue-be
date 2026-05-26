/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.exception;

import org.springframework.http.HttpStatus;

import com.sku.aissue.exception.model.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {
  INVALID_INPUT_VALUE("GLOBAL4001", "유효하지 않은 입력입니다.", HttpStatus.BAD_REQUEST),
  RESOURCE_NOT_FOUND("GLOBAL4002", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  DUPLICATE_RESOURCE("GLOBAL4003", "이미 존재하는 리소스입니다.", HttpStatus.CONFLICT),
  FORBIDDEN("GLOBAL4004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
  INTERNAL_SERVER_ERROR("GLOBAL5001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  ;

  private final String code;
  private final String message;
  private final HttpStatus status;
}
