/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import java.time.LocalDateTime;

import com.sku.aissue.domain.entity.Content;
import com.sku.aissue.domain.entity.ContentSource;
import com.sku.aissue.domain.entity.ContentType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "콘텐츠 응답")
public class ContentResponse {

  @Schema(description = "콘텐츠 ID", example = "42")
  private Long id;

  @Schema(description = "제목", example = "트럼프, 관세 협상 타결 임박…한국도 포함")
  private String title;

  @Schema(description = "본문 요약", example = "미국 트럼프 대통령이 주요 교역국과의 관세 협상이 마무리 단계에 접어들었다고 밝혔다.")
  private String body;

  @Schema(
      description = "원본 URL",
      example = "https://www.hani.co.kr/arti/economy/economy_general/1234567.html")
  private String url;

  @Schema(description = "출처", example = "한겨레")
  private String source;

  @Schema(description = "카테고리", example = "null")
  private String category;

  @Schema(description = "콘텐츠 타입 (NEWS / ISSUE)", example = "NEWS")
  private ContentType contentType;

  @Schema(description = "수집 출처 (NAVER_NEWS / RSS / USER_SUBMITTED)", example = "RSS")
  private ContentSource contentSource;

  @Schema(description = "원본 발행 시각", example = "2026-04-28T10:30:00")
  private LocalDateTime publishedAt;

  @Schema(description = "수집 시각", example = "2026-04-28T10:35:12")
  private LocalDateTime collectedAt;

  public static ContentResponse from(Content content) {
    return ContentResponse.builder()
        .id(content.getId())
        .title(content.getTitle())
        .body(content.getBody())
        .url(content.getUrl())
        .source(content.getSource())
        .category(content.getCategory())
        .contentType(content.getContentType())
        .contentSource(content.getContentSource())
        .publishedAt(content.getPublishedAt())
        .collectedAt(content.getCollectedAt())
        .build();
  }
}
