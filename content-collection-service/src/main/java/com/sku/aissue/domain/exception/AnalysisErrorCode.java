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
  NO_TRENDING_CONTENT("ANALYSIS4001", "현재 트렌딩 콘텐츠가 없습니다.", HttpStatus.NOT_FOUND),
  ANALYSIS_NOT_FOUND("ANALYSIS4041", "아직 분석되지 않은 콘텐츠입니다.", HttpStatus.NOT_FOUND),
  INVALID_URL("ANALYSIS4002", "올바르지 않은 URL 형식입니다.", HttpStatus.BAD_REQUEST),
  URL_NOT_ACCESSIBLE("ANALYSIS4003", "URL에 접근할 수 없습니다. 주소를 확인해주세요.", HttpStatus.BAD_REQUEST),
  NOT_ARTICLE_URL("ANALYSIS4004", "뉴스 기사 본문을 찾을 수 없는 URL입니다.", HttpStatus.BAD_REQUEST),
  AI_API_ERROR("ANALYSIS5001", "AI API 호출에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  AI_PARSE_ERROR("ANALYSIS5002", "AI 응답을 파싱할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  ;

  private final String code;
  private final String message;
  private final HttpStatus status;
}
