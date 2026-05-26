/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import com.sku.aissue.domain.entity.CharacterItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "착용 중인 아이템 정보")
public class EquippedItemResponse {

  @Schema(description = "아이템 키", example = "MANGO_HAT")
  private String key;

  @Schema(description = "아이템 이름", example = "망고 모자")
  private String name;

  public static EquippedItemResponse from(CharacterItem item) {
    if (item == null) return null;
    return EquippedItemResponse.builder().key(item.name()).name(item.getDisplayName()).build();
  }
}
