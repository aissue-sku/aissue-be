/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "급상승 키워드")
public class TrendingKeywordResponse {

  @Schema(description = "순위", example = "1")
  private final int rank;

  @Schema(description = "키워드", example = "전기차 화재")
  private final String keyword;

  @Schema(description = "최근 1시간 언급 수", example = "45")
  private final int count;

  @Schema(description = "급상승 여부 (surgeRatio ≥ 3.0 또는 데이터 부족 시 상위 3위)", example = "true")
  private final boolean hot;

  @Schema(description = "구독 여부 (비로그인 시 항상 false)", example = "false")
  private final boolean subscribed;
}
