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
}
