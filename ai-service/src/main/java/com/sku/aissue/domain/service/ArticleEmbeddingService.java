/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sku.aissue.global.client.ContentServiceClient;
import com.sku.aissue.global.client.ContentServiceClient.ContentInfo;
import com.sku.aissue.global.client.OpenAiClient;
import com.sku.aissue.global.client.QdrantClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleEmbeddingService {

  private static final int BODY_EXCERPT_LENGTH = 500;

  private final OpenAiClient openAiClient;
  private final QdrantClient qdrantClient;
  private final ContentServiceClient contentServiceClient;

  @Async
  public void embedBatch(List<Long> contentIds) {
    if (contentIds == null || contentIds.isEmpty()) return;
    List<ContentInfo> contents = contentServiceClient.findByIds(contentIds);
    log.info("기사 임베딩 시작 - {}건", contents.size());
    int success = 0;
    for (ContentInfo content : contents) {
      try {
        embedOne(content);
        success++;
      } catch (Exception e) {
        log.warn("기사 임베딩 실패 - contentId: {}, error: {}", content.id(), e.getMessage());
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
      ContentServiceClient.PagedContentInfo pageResult = contentServiceClient.findPaged(page, 50);
      if (pageResult.contents().isEmpty()) break;

      for (ContentInfo content : pageResult.contents()) {
        try {
          embedOne(content);
          totalSuccess++;
        } catch (Exception e) {
          log.warn("일괄 임베딩 실패 - contentId: {}, error: {}", content.id(), e.getMessage());
          totalFail++;
        }
      }

      log.info(
          "일괄 임베딩 진행 중 - 페이지: {}/{}, 누적 성공: {}건", page + 1, pageResult.totalPages(), totalSuccess);

      if (!pageResult.hasNext()) break;
      page++;
    }

    log.info("전체 기사 일괄 임베딩 완료 - 성공: {}건, 실패: {}건", totalSuccess, totalFail);
  }

  private void embedOne(ContentInfo content) {
    String text = buildEmbedText(content);
    List<Float> vector = openAiClient.embed(text);

    Map<String, Object> payload =
        Map.of(
            "contentId", content.id(),
            "title", content.title() != null ? content.title() : "",
            "url", content.url() != null ? content.url() : "");

    qdrantClient.upsert(content.id(), vector, payload);
  }

  private String buildEmbedText(ContentInfo content) {
    String title = content.title() != null ? content.title() : "";
    String body = content.body() != null ? content.body() : "";
    String excerpt =
        body.length() > BODY_EXCERPT_LENGTH ? body.substring(0, BODY_EXCERPT_LENGTH) : body;
    return title + "\n" + excerpt;
  }
}
