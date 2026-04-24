/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@Schema(title = "SignUpRequest DTO", description = "사용자 회원가입을 위한 데이터 전송")
public class SignUpRequest {

  @NotBlank(message = "아이디 항목은 필수입니다.")
  @Size(min = 4, max = 20, message = "로그인 아이디는 4~20자여야 합니다.")
  @Pattern(regexp = "^[A-Za-z0-9]+$", message = "아이디는 영문과 숫자만 사용할 수 있습니다.")
  @Schema(description = "아이디", example = "aissue", minLength = 4, maxLength = 20)
  private String username;

  @NotBlank(message = "비밀번호 항목은 필수입니다.")
  @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*?&])[A-Za-z\\d$@$!%*?&]{8,}$",
      message = "비밀번호는 최소 8자 이상, 숫자 및 특수문자를 포함해야 합니다.")
  @Schema(description = "비밀번호", example = "qwer1234!", minLength = 8, maxLength = 20)
  private String password;

  @NotBlank(message = "이름 항목은 필수입니다.")
  @Size(min = 2, max = 20, message = "이름은 2~20자여야 합니다.")
  @Pattern(regexp = "^[가-힣A-Za-z]+$", message = "이름은 한글 또는 영문만 가능합니다.")
  @Schema(description = "사용자 이름", example = "아이슈", minLength = 2, maxLength = 20)
  private String name;
}
