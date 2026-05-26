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
public enum NotificationErrorCode implements BaseErrorCode {
  NOTIFICATION_NOT_FOUND("NOTI4041", "존재하지 않는 알림입니다.", HttpStatus.NOT_FOUND),
  ALREADY_SUBSCRIBED("NOTI4091", "이미 구독 중인 키워드입니다.", HttpStatus.CONFLICT),
  SUBSCRIPTION_NOT_FOUND("NOTI4042", "구독 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  UNAUTHORIZED("NOTI4011", "본인의 알림만 접근할 수 있습니다.", HttpStatus.UNAUTHORIZED),
  ;

  private final String code;
  private final String message;
  private final HttpStatus status;
}
