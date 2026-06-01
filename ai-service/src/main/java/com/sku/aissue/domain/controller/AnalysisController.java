/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.request.ContentSubmitRequest;
import com.sku.aissue.domain.dto.response.AnalysisHistoryResponse;
import com.sku.aissue.domain.dto.response.AnalysisScoreResponse;
import com.sku.aissue.domain.dto.response.ArticleCritiqueResponse;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AI 분석", description = "콘텐츠 신뢰도 분석 및 비평 API")
@RequestMapping("/api/analysis")
public interface AnalysisController {

  @PostMapping("/submit")
  @Operation(summary = "콘텐츠 신뢰도 분석 요청", description = "URL 또는 텍스트를 제출하여 신뢰도 분석을 요청합니다. **로그인 필요.**")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "분석 완료"),
    @ApiResponse(responseCode = "400", description = "입력값 누락"),
    @ApiResponse(responseCode = "401", description = "인증 실패")
  })
  ResponseEntity<BaseResponse<AnalysisScoreResponse>> submit(
      @RequestBody @Valid ContentSubmitRequest request, @AuthenticationPrincipal String username);

  @GetMapping("/history")
  @Operation(summary = "신뢰도 분석 이력 조회", description = "내가 분석 요청한 콘텐츠 목록과 점수를 반환합니다. **로그인 필요.**")
  ResponseEntity<BaseResponse<List<AnalysisHistoryResponse>>> getAnalysisHistory(
      @AuthenticationPrincipal String username);

  @PostMapping("/critique")
  @Operation(summary = "기사 비평 분석", description = "기사의 신뢰 근거와 글쓰기 패턴을 분석합니다. **로그인 필요.**")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "분석 완료"),
    @ApiResponse(responseCode = "401", description = "인증 실패")
  })
  ResponseEntity<BaseResponse<ArticleCritiqueResponse>> critique(
      @RequestBody @Valid ContentSubmitRequest request, @AuthenticationPrincipal String username);

  @PostMapping("/embed/all")
  @SecurityRequirements
  @Operation(
      summary = "전체 기사 일괄 임베딩 (RAG 초기화)",
      description = "DB에 저장된 전체 기사를 Qdrant에 임베딩합니다. 비동기로 처리됩니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "임베딩 시작됨")})
  ResponseEntity<BaseResponse<Void>> embedAllArticles();

  @GetMapping("/content/{id}/critique")
  @SecurityRequirements
  @Operation(
      summary = "기사 비평 조회 (사전 계산)",
      description = "스케줄러가 사전 계산한 비평 분석 결과를 반환합니다. **인증 불필요.**")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "분석 결과 없음")
  })
  ResponseEntity<BaseResponse<ArticleCritiqueResponse>> getCritiqueByContentId(
      @Parameter(description = "콘텐츠 ID", example = "42") @PathVariable Long id);

  @GetMapping("/content/{id}/score")
  @SecurityRequirements
  @Operation(
      summary = "기사 신뢰도 분석 조회 (사전 계산)",
      description = "스케줄러가 사전 계산한 신뢰도 점수 분석 결과를 반환합니다. **인증 불필요.**")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "분석 결과 없음")
  })
  ResponseEntity<BaseResponse<AnalysisScoreResponse>> getAnalysisByContentId(
      @Parameter(description = "콘텐츠 ID", example = "42") @PathVariable Long id);
}
