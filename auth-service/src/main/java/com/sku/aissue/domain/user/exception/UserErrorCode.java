/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.user.exception;

import org.springframework.http.HttpStatus;

import com.sku.aissue.exception.model.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
  USER_NOT_FOUND("USER4001", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
  ;

  private final String code;
  private final String message;
  private final HttpStatus status;
}
