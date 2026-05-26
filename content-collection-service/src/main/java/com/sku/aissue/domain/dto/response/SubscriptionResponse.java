/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import java.time.LocalDateTime;

import com.sku.aissue.domain.entity.Subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "구독 정보")
public class SubscriptionResponse {

  @Schema(description = "구독 ID", example = "1")
  private final Long id;

  @Schema(description = "구독 키워드", example = "트럼프")
  private final String keyword;

  @Schema(description = "구독 등록일")
  private final LocalDateTime createdAt;

  public static SubscriptionResponse from(Subscription subscription) {
    return SubscriptionResponse.builder()
        .id(subscription.getId())
        .keyword(subscription.getKeyword())
        .createdAt(subscription.getCreatedAt())
        .build();
  }
}
