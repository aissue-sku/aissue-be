/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.request.SignUpRequest;
import com.sku.aissue.domain.dto.response.UserDetailResponse;
import com.sku.aissue.domain.dto.response.UserResponse;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "사용자", description = "사용자 관련 API")
@RequestMapping("/api/users")
public interface UserController {

  @PostMapping("sign-up")
  @Operation(summary = "회원가입", description = "사용자 회원가입을 처리합니다.")
  ResponseEntity<BaseResponse<UserResponse>> signUp(
      @Parameter(description = "회원가입 정보") @RequestBody @Valid SignUpRequest signUpRequest);

  @GetMapping
  @Operation(summary = "사용자 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
  ResponseEntity<BaseResponse<UserResponse>> getUserDetail();

  @GetMapping("/dev")
  @Operation(summary = "[개발자] 사용자 전체 조회", description = "사용자 리스트 관리를 위해 전체 사용자를 조회합니다.")
  ResponseEntity<BaseResponse<List<UserDetailResponse>>> getAllUsers();

  @DeleteMapping
  @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자의 계정을 삭제합니다. (Hard Delete)")
  ResponseEntity<BaseResponse<Void>> deleteUser(HttpServletResponse response);
}
