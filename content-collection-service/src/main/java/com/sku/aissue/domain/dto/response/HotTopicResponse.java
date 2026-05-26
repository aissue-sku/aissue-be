/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "급상승 키워드")
public class HotTopicResponse {

  @Schema(description = "키워드", example = "트럼프")
  private final String keyword;

  @Schema(description = "최근 1시간 언급 수", example = "12")
  private final int recentCount;

  @Schema(description = "급상승 배율 (최근 1시간 / 직전 23시간 시간당 평균)", example = "4.2")
  private final double surgeRatio;

  @Schema(description = "관련 기사 제목 샘플 (최대 3개)")
  private final List<String> sampleTitles;
}
