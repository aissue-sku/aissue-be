/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "콘텐츠 제출 요청 (신뢰도 분석 요청)")
public class ContentSubmitRequest {

  @NotNull @Schema(description = "제출 타입 (URL / TEXT)", example = "URL")
  private SubmitType type;

  @NotBlank
  @Schema(description = "분석할 URL 또는 텍스트", example = "https://news.example.com/article/123")
  private String content;

  public enum SubmitType {
    URL,
    TEXT
  }
}
