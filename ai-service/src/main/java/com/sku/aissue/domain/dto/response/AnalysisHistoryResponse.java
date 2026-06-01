/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import java.time.LocalDateTime;

import com.sku.aissue.domain.entity.ContentAnalysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "분석 이력")
public class AnalysisHistoryResponse {

  @Schema(description = "분석 ID", example = "1")
  private final Long id;

  @Schema(description = "콘텐츠 ID (상세 조회 시 사용)", example = "42")
  private final Long contentId;

  @Schema(description = "분석한 콘텐츠 제목", example = "트럼프, 관세 25% 전격 발표")
  private final String title;

  @Schema(description = "원문 URL", example = "https://news.example.com/article/123")
  private final String url;

  @Schema(description = "종합 점수", example = "72")
  private final int totalScore;

  @Schema(description = "종합 판단", example = "신뢰할 수 있음")
  private final String verdict;

  @Schema(description = "분석 일시")
  private final LocalDateTime analyzedAt;

  public static AnalysisHistoryResponse from(ContentAnalysis analysis, Long contentId) {
    return AnalysisHistoryResponse.builder()
        .id(analysis.getId())
        .contentId(contentId)
        .title(analysis.getTitle())
        .url(analysis.getUrl())
        .totalScore(analysis.getTotalScore())
        .verdict(analysis.getVerdict())
        .analyzedAt(analysis.getAnalyzedAt())
        .build();
  }
}
