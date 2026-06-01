/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.sku.aissue.global.config.QdrantProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class QdrantClient {

  private static final int VECTOR_DIM = 1536;
  private static final float SCORE_THRESHOLD = 0.4f;

  private final WebClient qdrantWebClient;
  private final String collection;

  public QdrantClient(
      @Qualifier("qdrantWebClient") WebClient qdrantWebClient, QdrantProperties props) {
    this.qdrantWebClient = qdrantWebClient;
    this.collection = props.getCollection();
  }

  @PostConstruct
  public void ensureCollection() {
    try {
      qdrantWebClient
          .get()
          .uri("/collections/{col}", collection)
          .retrieve()
          .toBodilessEntity()
          .block();
      log.info("Qdrant 컬렉션 확인 완료 - collection: {}", collection);
    } catch (WebClientResponseException.NotFound e) {
      createCollection();
    } catch (Exception e) {
      log.warn("Qdrant 연결 실패 (벡터 검색 비활성화) - error: {}", e.getMessage());
    }
  }

  private void createCollection() {
    try {
      Map<String, Object> body =
          Map.of("vectors", Map.of("size", VECTOR_DIM, "distance", "Cosine"));
      qdrantWebClient
          .put()
          .uri("/collections/{col}", collection)
          .bodyValue(body)
          .retrieve()
          .toBodilessEntity()
          .block();
      log.info("Qdrant 컬렉션 생성 완료 - collection: {}", collection);
    } catch (Exception e) {
      log.error("Qdrant 컬렉션 생성 실패 - error: {}", e.getMessage());
    }
  }

  public void upsert(Long contentId, List<Float> vector, Map<String, Object> payload) {
    try {
      Map<String, Object> point = Map.of("id", contentId, "vector", vector, "payload", payload);
      Map<String, Object> body = Map.of("points", List.of(point));
      qdrantWebClient
          .put()
          .uri("/collections/{col}/points", collection)
          .bodyValue(body)
          .retrieve()
          .toBodilessEntity()
          .block();
    } catch (Exception e) {
      log.error("Qdrant upsert 실패 - contentId: {}, error: {}", contentId, e.getMessage());
    }
  }

  public List<Long> search(List<Float> vector, int limit) {
    try {
      Map<String, Object> body =
          Map.of(
              "vector", vector,
              "limit", limit,
              "with_payload", true,
              "score_threshold", SCORE_THRESHOLD);

      SearchResponse response =
          qdrantWebClient
              .post()
              .uri("/collections/{col}/points/search", collection)
              .bodyValue(body)
              .retrieve()
              .bodyToMono(SearchResponse.class)
              .block();

      if (response == null || response.getResult() == null) return Collections.emptyList();

      return response.getResult().stream()
          .map(hit -> ((Number) hit.getPayload().get("contentId")).longValue())
          .toList();
    } catch (Exception e) {
      log.warn("Qdrant 검색 실패 - error: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  @Getter
  @NoArgsConstructor
  static class SearchResponse {
    private List<SearchHit> result;

    @Getter
    @NoArgsConstructor
    static class SearchHit {
      private Long id;
      private double score;
      private Map<String, Object> payload;
    }
  }
}
