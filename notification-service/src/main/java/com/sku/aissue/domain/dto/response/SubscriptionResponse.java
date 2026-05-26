/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "구독 키워드 정보")
public class SubscriptionResponse {

  @Schema(description = "키워드", example = "삼성전자")
  private String keyword;

  @Schema(description = "구독 등록 시각")
  private LocalDateTime subscribedAt;
}
