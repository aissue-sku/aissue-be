/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "사용자 권한 Enum")
public enum Role {
  @Schema(description = "사용자")
  ROLE_USER("사용자"),
  @Schema(description = "관리자")
  ROLE_ADMIN("관리자"),
  @Schema(description = "개발자")
  ROLE_DEVELOPER("개발자");

  private final String ko;
}
