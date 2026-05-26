/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import com.sku.aissue.domain.entity.CharacterItem;
import com.sku.aissue.domain.entity.ItemType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "아이템 정보 (구매 여부 포함)")
public class ItemInfoResponse {

  @Schema(description = "아이템 키", example = "MANGO_HAT")
  private String key;

  @Schema(description = "아이템 카테고리", example = "HAT")
  private ItemType type;

  @Schema(description = "아이템 이름", example = "망고 모자")
  private String name;

  @Schema(description = "구매 여부", example = "false")
  private boolean purchased;

  @Schema(description = "가격 (기본 제공 아이템은 0)", example = "200")
  private int price;

  public static ItemInfoResponse of(CharacterItem item, boolean purchased) {
    return ItemInfoResponse.builder()
        .key(item.name())
        .type(item.getType())
        .name(item.getDisplayName())
        .purchased(purchased)
        .price(item.getPrice())
        .build();
  }
}
