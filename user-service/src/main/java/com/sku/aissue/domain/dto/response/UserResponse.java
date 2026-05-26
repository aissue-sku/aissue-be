/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "UserResponse DTO", description = "사용자 정보 응답 반환")
public class UserResponse {

  @Schema(description = "사용자 식별자", example = "1")
  private Long userId;

  @Schema(description = "사용자 아이디", example = "aissue")
  private String username;

  @Schema(description = "사용자 이름", example = "아이슈")
  private String name;

  @Schema(description = "선택된 색상 테마 키", example = "AISSUE")
  private String color;

  @Schema(description = "보유 포인트", example = "500")
  private int points;
}
