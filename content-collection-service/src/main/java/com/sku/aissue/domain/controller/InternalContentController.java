/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
    name = "internal-content-controller-impl",
    description = "내부 서비스 간 콘텐츠 API (X-Internal-Token 필요)")
@RequestMapping("/internal/contents")
public interface InternalContentController {

  @GetMapping("/hot-keywords")
  @Operation(summary = "급상승 키워드 목록 조회", description = "최근 급상승 키워드 문자열 목록을 반환합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
  ResponseEntity<List<String>> getHotKeywords();

  @GetMapping("/{id}")
  @Operation(summary = "콘텐츠 단건 조회", description = "콘텐츠 ID로 단건 조회합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "콘텐츠 없음")
  })
  ResponseEntity<ContentInfo> getById(
      @Parameter(description = "콘텐츠 ID", example = "42") @PathVariable Long id);

  @GetMapping("/by-ids")
  @Operation(summary = "콘텐츠 다건 조회", description = "콘텐츠 ID 목록으로 다건 조회합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
  ResponseEntity<List<ContentInfo>> getByIds(
      @Parameter(description = "콘텐츠 ID 목록", example = "1,2,3") @RequestParam List<Long> ids);

  @GetMapping("/by-title")
  @Operation(summary = "제목 키워드 검색", description = "제목에 키워드가 포함된 콘텐츠를 최신순으로 반환합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "검색 성공")})
  ResponseEntity<List<ContentInfo>> getByTitle(
      @Parameter(description = "검색 키워드", example = "트럼프") @RequestParam String keyword,
      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10")
          int size);

  @GetMapping("/paged")
  @Operation(summary = "콘텐츠 페이지 조회", description = "전체 콘텐츠를 페이지 단위로 반환합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
  ResponseEntity<PagedContentInfo> getPaged(
      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "페이지 크기", example = "100") @RequestParam(defaultValue = "100")
          int size);

  @PostMapping("/save-or-find")
  @Operation(summary = "콘텐츠 저장 또는 조회", description = "URL 기준으로 기존 콘텐츠를 찾거나 없으면 새로 저장합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "저장 또는 조회 성공")})
  ResponseEntity<ContentInfo> saveOrFind(@RequestBody SaveContentRequest request);

  @GetMapping("/by-url")
  @Operation(summary = "URL로 콘텐츠 ID 조회", description = "콘텐츠 URL로 해당 콘텐츠의 ID를 반환합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "콘텐츠 없음")
  })
  ResponseEntity<Long> getIdByUrl(
      @Parameter(description = "콘텐츠 URL", example = "https://n.news.naver.com/article/001/0001")
          @RequestParam
          String url);

  record ContentInfo(
      Long id, String title, String body, String url, String source, LocalDateTime publishedAt) {}

  record PagedContentInfo(List<ContentInfo> contents, boolean hasNext, int totalPages) {}

  record SaveContentRequest(
      String title,
      String body,
      String url,
      String source,
      String contentType,
      String contentSource) {}
}
