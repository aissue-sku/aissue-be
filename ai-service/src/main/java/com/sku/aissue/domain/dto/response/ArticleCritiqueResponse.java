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
@Schema(description = "기사 비평 분석 결과")
public class ArticleCritiqueResponse {

  @Schema(description = "신뢰할 수 있는 근거 목록")
  private final List<String> trustReasons;

  @Schema(description = "비평 항목 목록")
  private final List<CritiqueItem> critiques;

  @Getter
  @Builder
  @Schema(description = "비평 항목")
  public static class CritiqueItem {

    @Schema(description = "패턴 이름", example = "과도한 접속사 패턴")
    private final String label;

    @Schema(description = "발견 횟수", example = "2")
    private final int count;

    @Schema(
        description = "중요도",
        example = "확인",
        allowableValues = {"주의", "확인", "참고"})
    private final String status;

    @Schema(description = "설명", example = "→ ~인듯하다, ~인 것 같다 같은 불확실한 표현 사용이 잦습니다")
    private final String description;
  }
}
