/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import com.sku.aissue.domain.entity.TrendingSnapshot;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "트렌딩 콘텐츠 응답")
public class TrendingContentResponse {

  @Schema(description = "트렌딩 순위 (1위부터)", example = "1")
  private Integer rankOrder;

  @Schema(description = "콘텐츠 정보")
  private ContentResponse content;

  public static TrendingContentResponse from(TrendingSnapshot snapshot) {
    return TrendingContentResponse.builder()
        .rankOrder(snapshot.getRankOrder())
        .content(ContentResponse.from(snapshot.getContent()))
        .build();
  }
}
