/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "키워드 구독 요청")
public class SubscriptionRequest {

  @NotBlank
  @Size(min = 1, max = 50)
  @Schema(description = "구독할 키워드", example = "트럼프")
  private String keyword;
}
