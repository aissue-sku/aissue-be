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
public enum AnalysisErrorCode implements BaseErrorCode {
  AI_API_ERROR(HttpStatus.BAD_GATEWAY, "AI_001", "AI API 호출에 실패했습니다."),
  AI_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI_002", "AI 응답 파싱에 실패했습니다."),
  ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "AI_003", "아직 분석되지 않은 콘텐츠입니다."),
  INVALID_URL(HttpStatus.BAD_REQUEST, "AI_004", "유효하지 않은 URL입니다."),
  URL_NOT_ACCESSIBLE(HttpStatus.BAD_REQUEST, "AI_005", "URL에 접근할 수 없습니다."),
  NOT_ARTICLE_URL(HttpStatus.BAD_REQUEST, "AI_006", "기사 본문을 추출할 수 없는 URL입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
