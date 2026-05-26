/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@Schema(description = "콘텐츠 신뢰도 분석 결과")
public class AnalysisScoreResponse {

  @Schema(description = "저장된 콘텐츠 ID (비평 조회 등 후속 요청에 사용)")
  private final Long contentId;

  @Schema(description = "원본 기사 URL")
  private final String url;

  @Schema(description = "종합 점수 (100점 만점)", example = "72")
  private final int totalScore;

  @Schema(description = "종합 판단", example = "신뢰할 수 있음")
  private final String verdict;

  @Schema(description = "종합 근거 요약")
  private final String summary;

  @Schema(description = "출처 신뢰도 항목 (25점 만점)")
  private final ScoreDetail credibility;

  @Schema(description = "내용 정확성 항목 (25점 만점)")
  private final ScoreDetail accuracy;

  @Schema(description = "편향성 분석 항목 (20점 만점, 높을수록 중립적)")
  private final ScoreDetail bias;

  @Schema(description = "교차 검증 항목 (20점 만점)")
  private final ScoreDetail crossVerification;

  @Schema(description = "투명성 지표 항목 (10점 만점)")
  private final ScoreDetail transparency;

  @Schema(description = "팩트체크 항목별 결과")
  private final List<FactCheckItem> factChecks;

  @Schema(description = "관련 기사 목록 (최대 3개)")
  private final List<RelatedArticle> relatedArticles;

  @Getter
  @Builder
  @Schema(description = "항목별 점수와 근거")
  public static class ScoreDetail {

    @Schema(description = "점수", example = "18")
    private final int score;

    @Schema(description = "해당 점수를 부여한 근거 (본문 내용 인용 포함)")
    private final String reason;

    @Schema(description = "하위 세부 항목 점수")
    private final List<SubItem> items;
  }

  @Getter
  @Builder
  @Schema(description = "세부 하위 항목 점수")
  public static class SubItem {

    @Schema(description = "항목 이름", example = "언론사 이력")
    private final String label;

    @Schema(description = "점수", example = "8")
    private final int score;
  }

  @Getter
  @Builder
  @Schema(description = "팩트체크 항목")
  public static class FactCheckItem {

    @Schema(description = "검증 대상 주장", example = "트럼프 대통령은 관세를 25%로 인상했다")
    private final String claim;

    @Schema(
        description = "검증 결과",
        example = "사실",
        allowableValues = {"사실", "불확실", "거짓"})
    private final String status;

    @Schema(description = "판단 근거", example = "백악관 공식 발표에서 25% 인상을 확인할 수 있습니다.")
    private final String evidence;
  }

  @Getter
  @Builder
  @Schema(description = "관련 기사")
  public static class RelatedArticle {

    @Schema(description = "기사 제목")
    private final String title;

    @Schema(description = "기사 URL")
    private final String url;

    @Schema(description = "언론사", example = "한국경제신문")
    private final String publisher;

    @Schema(description = "발행 시간 (예: 5분 전)", example = "2시간 전")
    private final String timeAgo;

    @Schema(description = "신뢰 점수 (분석 이력 없으면 null)", example = "88")
    private final Integer trustScore;
  }
}
