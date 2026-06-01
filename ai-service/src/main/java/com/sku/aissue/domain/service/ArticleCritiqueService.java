/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sku.aissue.domain.dto.response.ArticleCritiqueResponse;
import com.sku.aissue.domain.dto.response.ArticleCritiqueResponse.CritiqueItem;
import com.sku.aissue.domain.entity.ArticleCritique;
import com.sku.aissue.domain.exception.AnalysisErrorCode;
import com.sku.aissue.domain.repository.ArticleCritiqueRepository;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.global.client.OpenAiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCritiqueService {

  private static final String SYSTEM_PROMPT =
      """
      You are a strict Korean journalism critic. Analyze the given Korean article and return:
      1. Reasons the article is trustworthy (up to 3 specific points)
      2. Writing patterns that may reduce credibility — check ALL 6 patterns aggressively

      Patterns to check (be thorough, even subtle occurrences count):
      - 불확실 표현: ~인 것 같다, ~인듯하다, ~로 보인다, ~할 수 있다, ~것으로 전해졌다 등
      - 반복 문장 구조: 유사한 문장 길이·패턴이 2회 이상 반복
      - 전환어 과다 사용: 따라서, 그러나, 한편, 또한, 더불어, 이에 따라 등 전환어
      - 출처 불명확: ~에 따르면, ~관계자, ~소식통, 익명, 복수의 관계자 등 모호한 출처
      - 감정적·선동적 표현: 충격, 논란, 파문, 경악, 분노 등 감정을 자극하는 표현
      - AI 생성 패턴: 지나치게 정형화된 구조, 나열식 문장, 과도하게 균형 잡힌 서술

      status rules:
      - "주의": count >= 3
      - "확인": count == 2
      - "참고": count == 1

      Respond ONLY in this JSON format:
      {
        "trustReasons": ["이유1", "이유2"],
        "critiques": [
          {
            "label": "패턴 이름",
            "count": <number>,
            "status": "주의 or 확인 or 참고",
            "description": "→ 구체적인 설명 (실제 표현 예시 포함)"
          }
        ]
      }

      Rules:
      - MUST find at least 1 critique pattern for any real news article — look harder if needed
      - If body is very short (title only), analyze based on title and infer from the writing style
      - trustReasons must cite specific content from the article, not generic praise
      - description must start with "→ " and quote actual text from the article
      - All text must be in Korean
      """;

  private final OpenAiClient openAiClient;
  private final ObjectMapper objectMapper;
  private final ArticleCritiqueRepository articleCritiqueRepository;

  @Transactional
  public void preComputeAndSave(String title, String body, String url, Long contentId) {
    if (url != null && articleCritiqueRepository.existsByUrl(url)) {
      log.info("비평 분석 캐시 존재 - 스킵 url: {}", url);
      return;
    }
    log.info("비평 분석 사전 계산 시작 - title: {}", title);
    try {
      String rawResponse = callGpt(title, body);
      save(url, contentId, rawResponse);
    } catch (Exception e) {
      log.error("비평 분석 사전 계산 실패 - title: {}, error: {}", title, e.getMessage());
    }
  }

  @Transactional
  public ArticleCritiqueResponse analyzeWithCache(String title, String body, String url) {
    if (url != null) {
      return articleCritiqueRepository
          .findTopByUrlOrderByAnalyzedAtDesc(url)
          .map(
              cached -> {
                log.info("비평 분석 캐시 반환 - url: {}", url);
                return parseResponse(cached.getResultJson());
              })
          .orElseGet(
              () -> {
                String rawResponse = callGpt(title, body);
                save(url, null, rawResponse);
                return parseResponse(rawResponse);
              });
    }
    return analyze(title, body);
  }

  public ArticleCritiqueResponse getByContentId(Long contentId) {
    return articleCritiqueRepository
        .findTopByContentIdOrderByAnalyzedAtDesc(contentId)
        .map(cached -> parseResponse(cached.getResultJson()))
        .orElseThrow(() -> new CustomException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));
  }

  public ArticleCritiqueResponse analyze(String title, String body) {
    log.info("비평 분석 시작 - title: {}", title);
    String rawResponse = callGpt(title, body);
    ArticleCritiqueResponse result = parseResponse(rawResponse);
    log.info("비평 분석 완료 - 비평 항목 수: {}", result.getCritiques().size());
    return result;
  }

  private String callGpt(String title, String body) {
    String userPrompt = buildPrompt(title, body);
    return openAiClient.chat(SYSTEM_PROMPT, userPrompt);
  }

  private void save(String url, Long contentId, String rawResponse) {
    if (url == null) return;
    ArticleCritique entity =
        ArticleCritique.builder()
            .url(url)
            .contentId(contentId)
            .resultJson(rawResponse)
            .analyzedAt(LocalDateTime.now())
            .build();
    articleCritiqueRepository.save(entity);
  }

  private String buildPrompt(String title, String body) {
    StringBuilder sb = new StringBuilder();
    sb.append("제목: ").append(title != null ? title : "").append("\n\n");
    if (body != null && !body.isBlank()) {
      String trimmed = body.length() > 3000 ? body.substring(0, 3000) + "..." : body;
      sb.append("본문:\n").append(trimmed);
    }
    return sb.toString();
  }

  ArticleCritiqueResponse parseResponse(String response) {
    try {
      JsonNode root = objectMapper.readTree(response);

      List<String> trustReasons = new ArrayList<>();
      JsonNode trustNode = root.path("trustReasons");
      if (trustNode.isArray()) {
        trustReasons = objectMapper.convertValue(trustNode, new TypeReference<>() {});
      }

      List<CritiqueItem> critiques = new ArrayList<>();
      JsonNode critiquesNode = root.path("critiques");
      if (critiquesNode.isArray()) {
        for (JsonNode item : critiquesNode) {
          critiques.add(
              CritiqueItem.builder()
                  .label(item.path("label").asText(""))
                  .count(item.path("count").asInt(1))
                  .status(item.path("status").asText("참고"))
                  .description(item.path("description").asText(""))
                  .build());
        }
      }

      if (critiques.isEmpty()) {
        critiques.add(
            CritiqueItem.builder()
                .label("출처 불명확")
                .count(1)
                .status("참고")
                .description("→ 구체적인 출처나 발언자가 명시되지 않은 표현이 포함될 수 있습니다.")
                .build());
      }

      return ArticleCritiqueResponse.builder()
          .trustReasons(trustReasons)
          .critiques(critiques)
          .build();

    } catch (JsonProcessingException e) {
      log.info("비평 분석 응답 파싱 실패 - error: {}", e.getMessage());
      throw new CustomException(AnalysisErrorCode.AI_PARSE_ERROR);
    }
  }
}
