/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sku.aissue.domain.entity.Content;
import com.sku.aissue.domain.repository.ContentRepository;
import com.sku.aissue.global.client.OpenAiClient;
import com.sku.aissue.global.client.QdrantClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleEmbeddingService {

  private static final int BODY_EXCERPT_LENGTH = 500;
  private static final int BATCH_SIZE = 50;

  private final OpenAiClient openAiClient;
  private final QdrantClient qdrantClient;
  private final ContentRepository contentRepository;

  @Async
  public void embedBatch(List<Content> contents) {
    log.info("기사 임베딩 시작 - {}건", contents.size());
    int success = 0;
    for (Content content : contents) {
      try {
        embedOne(content);
        success++;
      } catch (Exception e) {
        log.warn("기사 임베딩 실패 - contentId: {}, error: {}", content.getId(), e.getMessage());
      }
    }
    log.info("기사 임베딩 완료 - 성공: {}건 / 전체: {}건", success, contents.size());
  }

  @Async
  public void embedAllExisting() {
    log.info("전체 기사 일괄 임베딩 시작");
    int page = 0;
    int totalSuccess = 0;
    int totalFail = 0;

    while (true) {
      Page<Content> pageResult = contentRepository.findAll(PageRequest.of(page, BATCH_SIZE));
      if (pageResult.isEmpty()) break;

      for (Content content : pageResult.getContent()) {
        try {
          embedOne(content);
          totalSuccess++;
        } catch (Exception e) {
          log.warn("일괄 임베딩 실패 - contentId: {}, error: {}", content.getId(), e.getMessage());
          totalFail++;
        }
      }

      log.info(
          "일괄 임베딩 진행 중 - 페이지: {}/{}, 누적 성공: {}건",
          page + 1,
          pageResult.getTotalPages(),
          totalSuccess);

      if (!pageResult.hasNext()) break;
      page++;
    }

    log.info("전체 기사 일괄 임베딩 완료 - 성공: {}건, 실패: {}건", totalSuccess, totalFail);
  }

  private void embedOne(Content content) {
    String text = buildEmbedText(content);
    List<Float> vector = openAiClient.embed(text);

    Map<String, Object> payload =
        Map.of(
            "contentId", content.getId(),
            "title", content.getTitle() != null ? content.getTitle() : "",
            "url", content.getUrl() != null ? content.getUrl() : "");

    qdrantClient.upsert(content.getId(), vector, payload);
  }

  private String buildEmbedText(Content content) {
    String title = content.getTitle() != null ? content.getTitle() : "";
    String body = content.getBody() != null ? content.getBody() : "";
    String excerpt =
        body.length() > BODY_EXCERPT_LENGTH ? body.substring(0, BODY_EXCERPT_LENGTH) : body;
    return title + "\n" + excerpt;
  }
}
