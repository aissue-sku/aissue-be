/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "LoginRequest DTO", description = "사용자 로그인을 위한 데이터 전송")
public class LoginRequest {

  @NotBlank(message = "로그인 아이디는 필수입니다.")
  @Size(min = 4, max = 20, message = "로그인 아이디는 4~20자여야 합니다.")
  @Schema(description = "아이디", example = "aissue", minLength = 4, maxLength = 20)
  private String username;

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
  @Schema(description = "비밀번호", example = "qwer1234!", minLength = 8, maxLength = 20)
  private String password;
}
