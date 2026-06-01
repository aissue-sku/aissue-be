/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sku.aissue.domain.dto.response.ContentResponse;
import com.sku.aissue.domain.dto.response.HotTopicResponse;
import com.sku.aissue.domain.dto.response.NewsItemResponse;
import com.sku.aissue.domain.dto.response.PopularKeywordResponse;
import com.sku.aissue.domain.dto.response.TrendingContentResponse;
import com.sku.aissue.domain.dto.response.TrendingKeywordResponse;
import com.sku.aissue.domain.entity.PeriodType;
import com.sku.aissue.global.page.InfiniteResponse;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "콘텐츠", description = "콘텐츠 수집 및 조회 API")
@RequestMapping("/api/contents")
public interface ContentController {

  @GetMapping("/trending")
  @SecurityRequirements
  @Operation(
      summary = "트렌딩 콘텐츠 조회",
      description =
          """
          현재 트렌딩 이슈/뉴스 목록을 순위 순으로 반환합니다. **인증 불필요.**
          - `HOURLY`: 최근 1시간 내 수집된 콘텐츠 기준 (매 시간 갱신)
          - `DAILY`: 최근 24시간 내 수집된 콘텐츠 기준 (매일 자정 갱신)
          """)
  @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
  ResponseEntity<BaseResponse<List<TrendingContentResponse>>> getTrending(
      @Parameter(description = "집계 주기 (HOURLY / DAILY)", example = "HOURLY")
          @RequestParam(defaultValue = "HOURLY")
          PeriodType periodType);

  @GetMapping("/{id}")
  @SecurityRequirements
  @Operation(summary = "콘텐츠 상세 조회", description = "콘텐츠 ID로 상세 정보를 조회합니다. **인증 불필요.**")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "콘텐츠 없음")
  })
  ResponseEntity<BaseResponse<ContentResponse>> getById(
      @Parameter(description = "콘텐츠 ID", example = "42") @PathVariable Long id);

  @GetMapping("/search")
  @SecurityRequirements
  @Operation(summary = "콘텐츠 검색", description = "제목 기준 키워드 검색 (대소문자 무시). **인증 불필요.**")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "검색 성공")})
  ResponseEntity<BaseResponse<List<ContentResponse>>> search(
      @Parameter(description = "검색 키워드", example = "트럼프") @RequestParam String keyword);

  @PostMapping("/trending/refresh")
  @SecurityRequirements
  @Operation(summary = "트렌딩 수동 갱신", description = "스케줄러를 기다리지 않고 트렌딩 스냅샷을 즉시 갱신합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "갱신 성공")})
  ResponseEntity<BaseResponse<Void>> refreshTrending();

  @PostMapping("/hot-topics/refresh")
  @SecurityRequirements
  @Operation(summary = "급상승 키워드 수동 갱신", description = "스케줄러를 기다리지 않고 급상승 키워드를 즉시 분석하여 DB에 저장합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "갱신 성공")})
  ResponseEntity<BaseResponse<List<HotTopicResponse>>> refreshHotTopics();

  @GetMapping("/keyword-news")
  @SecurityRequirements
  @Operation(
      summary = "키워드 관련 뉴스 조회 (무한스크롤)",
      description =
          """
          키워드가 제목에 포함된 뉴스를 최신순으로 반환합니다. **인증 불필요.**
          - 첫 요청: `cursor` 생략
          - 다음 요청: 이전 응답의 `lastCursor` 값을 `cursor`로 전달
          - `hasNext: false`이면 마지막 페이지입니다.
          """)
  @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
  ResponseEntity<BaseResponse<InfiniteResponse<NewsItemResponse>>> getKeywordNews(
      @Parameter(description = "검색 키워드", example = "전기차 화재") @RequestParam String keyword,
      @Parameter(description = "커서 (이전 응답의 lastCursor, 첫 요청 시 생략)") @RequestParam(required = false)
          Long cursor,
      @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10")
          int size);

  @GetMapping("/popular-keywords")
  @SecurityRequirements
  @Operation(summary = "인기 검색어 Top 3", description = "사용자가 가장 많이 구독한 키워드 상위 3개를 반환합니다. **인증 불필요.**")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
  ResponseEntity<BaseResponse<List<PopularKeywordResponse>>> getPopularKeywords();

  @GetMapping("/hot-topics")
  @SecurityRequirements
  @Operation(
      summary = "급상승 키워드 조회",
      description =
          """
          최근 1시간 수집량이 평소 대비 급증한 키워드 목록을 반환합니다. **인증 불필요.**
          - 로그인 상태이면 `subscribed` 필드에 구독 여부가 반영됩니다.
          - 비로그인 시 `subscribed`는 항상 `false`입니다.
          """)
  ResponseEntity<BaseResponse<List<TrendingKeywordResponse>>> getHotTopics(
      @AuthenticationPrincipal String username);
}
