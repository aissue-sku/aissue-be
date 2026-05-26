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
@Schema(description = "캐릭터 전체 정보 응답")
public class CharacterResponse {

  @Schema(description = "현재 착용 중인 아이템")
  private EquippedResponse equipped;

  @Schema(description = "전체 아이템 목록 (구매 여부 포함)")
  private List<ItemInfoResponse> items;

  @Getter
  @Builder
  @Schema(description = "착용 중인 아이템 세트")
  public static class EquippedResponse {

    @Schema(description = "착용 모자 (없으면 null)")
    private EquippedItemResponse hat;

    @Schema(description = "착용 얼굴")
    private EquippedItemResponse face;

    @Schema(description = "착용 옷 (없으면 null)")
    private EquippedItemResponse clothes;

    @Schema(description = "적용 색상")
    private EquippedItemResponse color;
  }
}
