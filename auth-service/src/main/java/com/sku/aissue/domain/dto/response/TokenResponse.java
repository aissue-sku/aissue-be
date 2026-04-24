/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 응답 DTO
 *
 * <p>인증 및 토큰 갱신 시 응답으로 사용되는 DTO입니다.
 *
 * @since 1.0.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 응답")
public class TokenResponse {

  @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String accessToken;

  @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
  private String refreshToken;

  @Schema(description = "사용자 아이디", example = "aissue")
  private String username;
}
