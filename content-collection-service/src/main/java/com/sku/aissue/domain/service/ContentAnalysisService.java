/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sku.aissue.domain.dto.response.AnalysisScoreResponse;
import com.sku.aissue.domain.dto.response.AnalysisScoreResponse.FactCheckItem;
import com.sku.aissue.domain.dto.response.AnalysisScoreResponse.RelatedArticle;
import com.sku.aissue.domain.dto.response.AnalysisScoreResponse.ScoreDetail;
import com.sku.aissue.domain.dto.response.AnalysisScoreResponse.SubItem;
import com.sku.aissue.domain.entity.Content;
import com.sku.aissue.domain.entity.ContentAnalysis;
import com.sku.aissue.domain.exception.AnalysisErrorCode;
import com.sku.aissue.domain.repository.ContentAnalysisRepository;
import com.sku.aissue.domain.repository.ContentRepository;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.global.client.OpenAiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentAnalysisService {

  // 실제 분석 이력이 없을 때 언론사 이름 기반 추정 신뢰 점수
  private static final Map<String, Integer> SOURCE_BASE_SCORES =
      Map.ofEntries(
          Map.entry("연합뉴스", 90),
          Map.entry("yonhapnews", 90),
          Map.entry("KBS", 88),
          Map.entry("MBC", 85),
          Map.entry("SBS", 85),
          Map.entry("조선일보", 82),
          Map.entry("중앙일보", 82),
          Map.entry("동아일보", 82),
          Map.entry("한겨레", 80),
          Map.entry("경향신문", 80),
          Map.entry("뉴시스", 82),
          Map.entry("뉴스1", 80),
          Map.entry("한국경제", 80),
          Map.entry("매일경제", 80),
          Map.entry("머니투데이", 78),
          Map.entry("조선비즈", 78),
          Map.entry("중앙선데이", 78),
          Map.entry("YTN", 85),
          Map.entry("JTBC", 83),
          Map.entry("TV조선", 75),
          Map.entry("채널A", 75),
          Map.entry("MBN", 75));

  private static final String SYSTEM_PROMPT =
      """
      You are a Korean content reliability analyzer. Analyze the given content and score it on 5 criteria.
      For EACH criterion, cite specific sentences or claims from the content that influenced the score.
      Also extract key verifiable claims and fact-check them.
      Respond ONLY in JSON format. All text fields must be written in Korean.

      Scoring criteria and sub-items:
      - credibility (max 25): 언론사 이력 (max 10) + 소유구조 (max 8) + 도메인 연령 (max 7)
      - accuracy (max 25): 팩트체크 (max 10) + 인용 검증 (max 8) + AI 생성 어투 (max 7)
      - bias (max 20): 감정적 표현 (max 7) + 정치 편향어 (max 7) + 헤드라인 일치 (max 6)
      - crossVerification (max 20): 타 매체 일치 (max 8) + 공식 기관 대조 (max 7) + 전문가 발언 (max 5)
      - transparency (max 10): 작성자 실명 (max 5) + 광고 표기 (max 3) + 수정 이력 (max 2)

      IMPORTANT: Each category's "score" must equal the sum of its sub-item scores.

      Response format:
      {
        "verdict": "신뢰할 수 있음 or 주의 필요 or 신뢰하기 어려움",
        "summary": "2-3 sentences overall summary in Korean",
        "credibility": {
          "score": <0-25>,
          "reason": "Cite specific quotes or claims. Explain why this score was given.",
          "items": [
            { "label": "언론사 이력", "score": <0-10> },
            { "label": "소유구조", "score": <0-8> },
            { "label": "도메인 연령", "score": <0-7> }
          ]
        },
        "accuracy": {
          "score": <0-25>,
          "reason": "Cite specific quotes or claims. Explain why this score was given.",
          "items": [
            { "label": "팩트체크", "score": <0-10> },
            { "label": "인용 검증", "score": <0-8> },
            { "label": "AI 생성 어투", "score": <0-7> }
          ]
        },
        "bias": {
          "score": <0-20>,
          "reason": "Cite specific emotional or biased expressions from the content.",
          "items": [
            { "label": "감정적 표현", "score": <0-7> },
            { "label": "정치 편향어", "score": <0-7> },
            { "label": "헤드라인 일치", "score": <0-6> }
          ]
        },
        "crossVerification": {
          "score": <0-20>,
          "reason": "Explain which claims can or cannot be verified with other sources.",
          "items": [
            { "label": "타 매체 일치", "score": <0-8> },
            { "label": "공식 기관 대조", "score": <0-7> },
            { "label": "전문가 발언", "score": <0-5> }
          ]
        },
        "transparency": {
          "score": <0-10>,
          "reason": "Explain whether sources, authors, and methods are clearly stated.",
          "items": [
            { "label": "작성자 실명", "score": <0-5> },
            { "label": "광고 표기", "score": <0-3> },
            { "label": "수정 이력", "score": <0-2> }
          ]
        },
        "factChecks": [
          {
            "claim": "Specific verifiable claim extracted from content",
            "status": "사실 or 불확실 or 거짓",
            "evidence": "Explain the verdict for this specific claim."
          }
        ]
      }
      """;

  private final OpenAiClient openAiClient;
  private final ObjectMapper objectMapper;
  private final ContentAnalysisRepository contentAnalysisRepository;
  private final ContentRepository contentRepository;

  /** 이슈 카드 상세 페이지 (3페이지): contentId로 사전 계산된 신뢰도 결과 반환 */
  public AnalysisScoreResponse getByContentId(Long contentId) {
    String url =
        contentRepository
            .findById(contentId)
            .map(content -> content.getUrl())
            .orElseThrow(() -> new CustomException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));

    return contentAnalysisRepository
        .findTopByUrlOrderByAnalyzedAtDesc(url)
        .map(
            cached -> {
              try {
                AnalysisScoreResponse result = parseResponse(cached.getDetailsJson());
                List<RelatedArticle> related = findRelatedArticles(cached.getTitle(), url);
                return result.toBuilder().url(url).relatedArticles(related).build();
              } catch (Exception e) {
                log.warn("신뢰도 분석 캐시 파싱 실패 - contentId: {}", contentId);
                throw new CustomException(AnalysisErrorCode.ANALYSIS_NOT_FOUND);
              }
            })
        .orElseThrow(() -> new CustomException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));
  }

  @Transactional
  public AnalysisScoreResponse analyze(String title, String url, String body, String userId) {
    log.info("콘텐츠 신뢰도 분석 요청 시작 - title: {}", title);

    // URL이 있으면 기존 분석 결과 캐시 확인
    if (url != null) {
      var cached = contentAnalysisRepository.findTopByUrlOrderByAnalyzedAtDesc(url);
      if (cached.isPresent() && cached.get().getDetailsJson() != null) {
        try {
          log.info("신뢰도 분석 캐시 반환 - url: {}", url);
          AnalysisScoreResponse cachedResult = parseResponse(cached.get().getDetailsJson());
          // 사용자 이력 저장 (userId가 있을 때만)
          if (userId != null) {
            saveHistory(title, url, userId, cachedResult, cached.get().getDetailsJson());
          }
          List<RelatedArticle> relatedArticles = findRelatedArticles(title, url);
          return cachedResult.toBuilder().url(url).relatedArticles(relatedArticles).build();
        } catch (Exception e) {
          log.warn("신뢰도 분석 캐시 파싱 실패, GPT 재분석 - url: {}", url);
        }
      }
    }

    String userPrompt = buildUserPrompt(title, body);
    String rawResponse = openAiClient.chat(SYSTEM_PROMPT, userPrompt);
    AnalysisScoreResponse result = parseResponse(rawResponse);

    saveHistory(title, url, userId, result, rawResponse);

    List<RelatedArticle> relatedArticles = findRelatedArticles(title, url);
    result = result.toBuilder().url(url).relatedArticles(relatedArticles).build();

    log.info("콘텐츠 신뢰도 분석 성공 - totalScore: {}", result.getTotalScore());
    return result;
  }

  private List<RelatedArticle> findRelatedArticles(String title, String currentUrl) {
    if (title == null || title.isBlank()) return List.of();

    String keyword = extractKeyword(title);
    if (keyword == null) return List.of();

    List<Content> candidates =
        contentRepository.findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(
            keyword, PageRequest.of(0, 10));

    return candidates.stream()
        .filter(c -> c.getUrl() == null || !c.getUrl().equals(currentUrl))
        .limit(3)
        .map(
            c -> {
              Integer trustScore =
                  c.getUrl() != null
                      ? contentAnalysisRepository
                          .findTopByUrlOrderByAnalyzedAtDesc(c.getUrl())
                          .map(ContentAnalysis::getTotalScore)
                          .orElse(estimateSourceScore(c.getSource()))
                      : estimateSourceScore(c.getSource());
              return RelatedArticle.builder()
                  .title(c.getTitle())
                  .url(c.getUrl())
                  .publisher(c.getSource())
                  .timeAgo(
                      toTimeAgo(
                          c.getPublishedAt() != null ? c.getPublishedAt() : c.getCollectedAt()))
                  .trustScore(trustScore)
                  .build();
            })
        .collect(Collectors.toList());
  }

  private int estimateSourceScore(String source) {
    if (source == null) return 65;
    return SOURCE_BASE_SCORES.entrySet().stream()
        .filter(e -> source.contains(e.getKey()))
        .mapToInt(Map.Entry::getValue)
        .findFirst()
        .orElse(65);
  }

  private String extractKeyword(String title) {
    return Arrays.stream(title.split("[^가-힣a-zA-Z0-9]+"))
        .filter(w -> w.length() >= 2)
        .findFirst()
        .orElse(null);
  }

  private String toTimeAgo(LocalDateTime dateTime) {
    if (dateTime == null) return "방금 전";
    long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
    if (minutes < 1) return "방금 전";
    if (minutes < 60) return minutes + "분 전";
    long hours = minutes / 60;
    if (hours < 24) return hours + "시간 전";
    long days = hours / 24;
    return days + "일 전";
  }

  private void saveHistory(
      String title, String url, String userId, AnalysisScoreResponse result, String detailsJson) {
    // 동일 사용자 + 동일 URL 중복 이력 방지
    if (userId != null
        && url != null
        && contentAnalysisRepository.existsByUserIdAndUrl(userId, url)) {
      log.info("분석 이력 중복 스킵 - userId: {}, url: {}", userId, url);
      return;
    }
    ContentAnalysis analysis =
        ContentAnalysis.builder()
            .userId(userId)
            .title(title)
            .url(url)
            .totalScore(result.getTotalScore())
            .credibilityScore(result.getCredibility().getScore())
            .accuracyScore(result.getAccuracy().getScore())
            .biasScore(result.getBias().getScore())
            .crossVerificationScore(result.getCrossVerification().getScore())
            .transparencyScore(result.getTransparency().getScore())
            .verdict(result.getVerdict())
            .summary(result.getSummary())
            .detailsJson(detailsJson)
            .analyzedAt(LocalDateTime.now())
            .build();
    contentAnalysisRepository.save(analysis);
  }

  private String buildUserPrompt(String title, String body) {
    StringBuilder sb = new StringBuilder();
    sb.append("Title: ").append(title != null ? title : "").append("\n\n");
    if (body != null && !body.isBlank()) {
      String trimmedBody = body.length() > 3000 ? body.substring(0, 3000) + "..." : body;
      sb.append("Content:\n").append(trimmedBody);
    }
    return sb.toString();
  }

  private AnalysisScoreResponse parseResponse(String response) {
    try {
      JsonNode root = objectMapper.readTree(response);

      ScoreDetail credibility = toDetail(root.path("credibility"), 25);
      ScoreDetail accuracy = toDetail(root.path("accuracy"), 25);
      ScoreDetail bias = toDetail(root.path("bias"), 20);
      ScoreDetail crossVerification = toDetail(root.path("crossVerification"), 20);
      ScoreDetail transparency = toDetail(root.path("transparency"), 10);
      List<FactCheckItem> factChecks = toFactChecks(root.path("factChecks"));

      int totalScore =
          credibility.getScore()
              + accuracy.getScore()
              + bias.getScore()
              + crossVerification.getScore()
              + transparency.getScore();

      return AnalysisScoreResponse.builder()
          .totalScore(totalScore)
          .verdict(root.path("verdict").asText("분석 불가"))
          .summary(root.path("summary").asText(""))
          .credibility(credibility)
          .accuracy(accuracy)
          .bias(bias)
          .crossVerification(crossVerification)
          .transparency(transparency)
          .factChecks(factChecks)
          .relatedArticles(List.of())
          .build();

    } catch (JsonProcessingException e) {
      log.info("분석 응답 파싱 실패 - error: {}", e.getMessage());
      throw new CustomException(AnalysisErrorCode.AI_PARSE_ERROR);
    }
  }

  private ScoreDetail toDetail(JsonNode node, int max) {
    int score = clamp(node.path("score").asInt(), 0, max);
    String reason = node.path("reason").asText("");
    List<SubItem> items = toSubItems(node.path("items"));
    return ScoreDetail.builder().score(score).reason(reason).items(items).build();
  }

  private List<SubItem> toSubItems(JsonNode node) {
    List<SubItem> items = new ArrayList<>();
    if (node.isArray()) {
      for (JsonNode item : node) {
        items.add(
            SubItem.builder()
                .label(item.path("label").asText(""))
                .score(item.path("score").asInt(0))
                .build());
      }
    }
    return items;
  }

  private List<FactCheckItem> toFactChecks(JsonNode node) {
    List<FactCheckItem> items = new ArrayList<>();
    if (node.isArray()) {
      for (JsonNode item : node) {
        items.add(
            FactCheckItem.builder()
                .claim(item.path("claim").asText(""))
                .status(item.path("status").asText("불확실"))
                .evidence(item.path("evidence").asText(""))
                .build());
      }
    }
    return items;
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
