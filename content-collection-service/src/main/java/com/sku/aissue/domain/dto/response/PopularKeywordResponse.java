/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "인기 검색어")
public class PopularKeywordResponse {

  @Schema(description = "키워드", example = "GTX 삼성역")
  private final String keyword;

  @Schema(description = "구독자 수", example = "42")
  private final long subscriberCount;
}
