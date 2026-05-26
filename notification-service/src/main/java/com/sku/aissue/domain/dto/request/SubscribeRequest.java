/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubscribeRequest {

  @NotBlank(message = "키워드를 입력해주세요.")
  @Size(max = 50, message = "키워드는 50자 이하여야 합니다.")
  private String keyword;
}
