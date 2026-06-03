/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.response.StockAnalysisResponse;
import com.sku.aissue.domain.service.ArticleEmbeddingService;
import com.sku.aissue.domain.service.IssueCardService;
import com.sku.aissue.domain.service.StockAnalysisService;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/internal/ai")
@Tag(name = "AI", description = "AI 생성 API")
@RequiredArgsConstructor
public class InternalAiController {

  private final IssueCardService issueCardService;
  private final ArticleEmbeddingService articleEmbeddingService;
  private final StockAnalysisService stockAnalysisService;

  @PostMapping("/issue-cards")
  public ResponseEntity<BaseResponse<Void>> triggerIssueCards() {
    log.info("이슈 카드 생성 트리거 수신");
    issueCardService.generateAndSaveByHotTopics();
    return ResponseEntity.ok(BaseResponse.success(null));
  }

  @PostMapping("/embed")
  public ResponseEntity<BaseResponse<Void>> triggerEmbed(@RequestBody List<Long> contentIds) {
    log.info("임베딩 트리거 수신 - {}건", contentIds != null ? contentIds.size() : 0);
    articleEmbeddingService.embedBatch(contentIds);
    return ResponseEntity.ok(BaseResponse.success(null));
  }

  @GetMapping("/stocks/analyze")
  public ResponseEntity<BaseResponse<List<StockAnalysisResponse>>> analyzeStocks(
      @RequestParam Long contentId) {
    log.info("주식 기술적 분석 요청 - contentId: {}", contentId);
    List<StockAnalysisResponse> result = stockAnalysisService.analyzeByContentId(contentId);
    return ResponseEntity.ok(BaseResponse.success(result));
  }

  @GetMapping("/stocks/{code}")
  public ResponseEntity<BaseResponse<StockAnalysisResponse>> analyzeStockByCode(
      @PathVariable String code) {
    log.info("주식 기술적 분석 요청 - code: {}", code);
    StockAnalysisResponse result = stockAnalysisService.analyzeByCode(code);
    return ResponseEntity.ok(BaseResponse.success(result));
  }
}
