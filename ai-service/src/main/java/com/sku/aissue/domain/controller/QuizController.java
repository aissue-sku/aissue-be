/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.request.QuizAnswerRequest;
import com.sku.aissue.domain.dto.response.QuizAnswerResponse;
import com.sku.aissue.domain.dto.response.QuizResponse;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "일일 퀴즈", description = "가짜 뉴스 탐지 퀴즈 API")
@RequestMapping("/api/quiz")
public interface QuizController {

  @GetMapping("/daily")
  @SecurityRequirements
  @Operation(
      summary = "오늘의 퀴즈 조회",
      description = "기사 3개(진짜 2개 + 가짜 1개)를 반환합니다. 로그인 시 참여 여부도 함께 반환합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
  ResponseEntity<BaseResponse<QuizResponse>> getTodayQuiz(@AuthenticationPrincipal String username);

  @PostMapping("/daily/generate")
  @SecurityRequirements
  @Operation(
      summary = "오늘의 퀴즈 수동 생성",
      description = "오늘의 퀴즈를 강제로 (재)생성합니다. 기존 퀴즈가 있으면 삭제 후 새로 만듭니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "생성 성공")})
  ResponseEntity<BaseResponse<QuizResponse>> generateTodayQuiz();

  @PostMapping("/daily/answer")
  @Operation(
      summary = "오늘의 퀴즈 정답 제출",
      description = "가짜 기사라고 생각하는 인덱스를 제출합니다. 정답 시 포인트를 획득합니다. **로그인 필요.**")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "제출 성공"),
    @ApiResponse(responseCode = "401", description = "인증 실패"),
    @ApiResponse(responseCode = "409", description = "이미 오늘 참여함")
  })
  ResponseEntity<BaseResponse<QuizAnswerResponse>> submitAnswer(
      @RequestBody @Valid QuizAnswerRequest request, @AuthenticationPrincipal String username);
}
