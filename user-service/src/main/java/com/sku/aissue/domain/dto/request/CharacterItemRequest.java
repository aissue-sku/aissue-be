/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "캐릭터 아이템 요청")
public class CharacterItemRequest {

  @NotBlank(message = "아이템 키는 필수입니다.")
  @Schema(description = "아이템 키", example = "MANGO_HAT")
  private String item;
}
