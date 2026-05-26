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
public enum UserErrorCode implements BaseErrorCode {
  USER_NOT_FOUND("USER4001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  UNAUTHORIZED("USER4002", "인증되지 않은 사용자입니다.", HttpStatus.UNAUTHORIZED),
  EXIST_USERNAME("USER4003", "이미 존재하는 아이디입니다.", HttpStatus.CONFLICT),

  ITEM_NOT_FOUND("USER4004", "존재하지 않는 아이템입니다.", HttpStatus.BAD_REQUEST),
  ITEM_ALREADY_PURCHASED("USER4005", "이미 구매한 아이템입니다.", HttpStatus.CONFLICT),
  ITEM_NOT_PURCHASED("USER4006", "구매하지 않은 아이템입니다.", HttpStatus.FORBIDDEN),
  ITEM_DEFAULT_OWNED("USER4007", "기본 제공 아이템은 구매할 수 없습니다.", HttpStatus.BAD_REQUEST),
  ITEM_UNEQUIP_NOT_ALLOWED("USER4008", "해당 카테고리는 해제할 수 없습니다.", HttpStatus.BAD_REQUEST),
  INSUFFICIENT_POINTS("USER4009", "포인트가 부족합니다.", HttpStatus.BAD_REQUEST),

  USER_SAVE_FAILED("USER5001", "회원 정보 저장에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  USER_DELETE_FAILED("USER5002", "사용자 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  ;

  private final String code;
  private final String message;
  private final HttpStatus status;
}
