/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.service.ArticleEmbeddingService;
import com.sku.aissue.domain.service.IssueCardService;
import com.sku.aissue.response.BaseResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
public class InternalAiController {

  private final IssueCardService issueCardService;
  private final ArticleEmbeddingService articleEmbeddingService;

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
}
