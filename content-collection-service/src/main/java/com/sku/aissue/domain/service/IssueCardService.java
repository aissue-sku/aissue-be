/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sku.aissue.domain.dto.response.IssueCardResponse;
import com.sku.aissue.domain.entity.Content;
import com.sku.aissue.domain.entity.IssueCard;
import com.sku.aissue.domain.entity.TrendingKeyword;
import com.sku.aissue.domain.exception.AnalysisErrorCode;
import com.sku.aissue.domain.repository.ContentRepository;
import com.sku.aissue.domain.repository.IssueCardRepository;
import com.sku.aissue.domain.repository.TrendingKeywordRepository;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.global.client.NotificationServiceClient;
import com.sku.aissue.global.client.OpenAiClient;
import com.sku.aissue.global.client.QdrantClient;
import com.sku.aissue.global.s3.S3ImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueCardService {

  private static final int SNAPSHOT_RETENTION_DAYS = 7;

  private static final String SYSTEM_PROMPT =
      """
      You are a Korean news card copywriter. For each given keyword and its related article, create exactly ONE issue card.

      Respond ONLY in this JSON format:
      {
        "cards": [
          {
            "tags": ["핵심키워드", "연관키워드1", "연관키워드2"],
            "hook": "Provocative Korean sentence (max 35 chars, end with . or !)",
            "teaser": "Curiosity-inducing Korean question (max 40 chars, must end with ?)",
            "category": "카테고리",
            "sourceUrl": "original URL",
            "imagePrompt": "English visual description for editorial illustration (max 20 words, no text, no specific faces)"
          }
        ]
      }

      tags rules:
      - First tag must be the given keyword itself (max 8 chars)
      - Second and third are related Korean noun concepts (max 8 chars each)

      Hook strategies (use at least one):
      - Shock/reversal: 알고 보니, 사실은, 결국, 터졌다, 충격 반전
      - Urgency: 지금 당장, 벼랑 끝, X일 안에, 이미 늦었나
      - Curiosity: 아무도 몰랐던, 숨겨진 진실, 진짜 이유
      - Specificity: numbers, amounts, timeframes

      Teaser rules:
      - Must be a question (end with ?)
      - Hook raises the tension; teaser asks the unanswered question that makes readers click
      - Examples: "진짜 피해자는 따로 있다고?", "왜 지금 이 뉴스가 터졌을까?", "당신의 지갑에도 영향이 올까?"

      category rules:
      - Choose exactly ONE from: 정치, 경제, 사회, 과학/IT, 문화/연예, 스포츠, 국제, 기타
      - Base on the main topic of the article

      imagePrompt rules:
      - Describe a scene or concept that visually represents the topic
      - Editorial/news illustration style
      - No text, no watermarks, no specific real persons' faces
      - In English

      Rules:
      - Generate EXACTLY one card per keyword provided — no more, no less
      - sourceUrl must be the exact URL provided for that keyword's article
      - hook must be declarative (no question marks), intense but related to the news
      """;

  private final IssueCardRepository issueCardRepository;
  private final TrendingKeywordRepository trendingKeywordRepository;
  private final ContentRepository contentRepository;
  private final OpenAiClient openAiClient;
  private final QdrantClient qdrantClient;
  private final NotificationServiceClient notificationServiceClient;
  private final ObjectMapper objectMapper;
  private final S3ImageService s3ImageService;
  private final ArticleCritiqueService articleCritiqueService;
  private final ContentAnalysisService contentAnalysisService;

  @Cacheable(value = "issueCards", key = "'latest'")
  public List<IssueCardResponse> getIssueCards() {
    log.info("이슈 카드 DB 조회 시작");
    List<IssueCard> cards = issueCardRepository.findLatest();

    if (cards.isEmpty()) {
      log.info("저장된 이슈 카드 없음");
      return Collections.emptyList();
    }

    log.info("이슈 카드 조회 성공 - 카드 수: {}", cards.size());
    return cards.stream().map(this::toResponse).toList();
  }

  public List<IssueCardResponse> getPersonalizedCards(String username) {
    if (username == null) {
      return getIssueCards();
    }

    List<String> subscribedKeywords = notificationServiceClient.getUserSubscribedKeywords(username);
    if (subscribedKeywords.isEmpty()) {
      return getIssueCards();
    }

    // 구독 키워드 카드 먼저, 나머지 최신 카드로 채움
    List<IssueCard> subscribed = issueCardRepository.findLatestByKeywords(subscribedKeywords);
    List<IssueCard> all = issueCardRepository.findLatest();

    java.util.Set<Long> subscribedIds =
        subscribed.stream().map(IssueCard::getId).collect(java.util.stream.Collectors.toSet());

    List<IssueCard> merged = new java.util.ArrayList<>(subscribed);
    all.stream().filter(c -> !subscribedIds.contains(c.getId())).forEach(merged::add);

    log.info(
        "개인화 이슈 카드 조회 - username: {}, 구독 매칭: {}개, 전체: {}개",
        username,
        subscribed.size(),
        merged.size());

    return merged.stream().map(this::toResponse).toList();
  }

  @Transactional
  @CacheEvict(value = "issueCards", allEntries = true)
  public void generateAndSaveByHotTopics() {
    log.info("키워드 기반 이슈 카드 생성 시작");

    // 급상승 키워드 + 인기 구독 키워드 병합 (중복 제거, 급상승 우선)
    List<String> keywords = buildKeywordList();
    if (keywords.isEmpty()) {
      log.info("처리할 키워드 없음 - 이슈 카드 생성 스킵");
      return;
    }

    // 키워드별 대표 기사 1개씩 조회 (RAG: Qdrant 벡터 검색 우선)
    Map<String, Content> keywordToContent = new LinkedHashMap<>();
    for (String keyword : keywords) {
      List<Content> articles = findArticleForKeyword(keyword);
      if (!articles.isEmpty()) {
        keywordToContent.put(keyword, articles.get(0));
      }
    }

    if (keywordToContent.isEmpty()) {
      log.info("키워드에 매칭되는 기사 없음 - 이슈 카드 생성 스킵");
      return;
    }

    String userPrompt = buildKeywordPrompt(keywordToContent);
    String aiResponse = openAiClient.chat(SYSTEM_PROMPT, userPrompt);
    List<GptCard> rawCards = parseGptResponse(aiResponse);

    Map<String, Content> urlToContent =
        keywordToContent.values().stream()
            .filter(c -> c.getUrl() != null)
            .collect(Collectors.toMap(Content::getUrl, c -> c, (a, b) -> a));

    // url → 해당 카드를 생성한 키워드 역방향 매핑
    Map<String, String> urlToKeyword =
        keywordToContent.entrySet().stream()
            .filter(e -> e.getValue().getUrl() != null)
            .collect(Collectors.toMap(e -> e.getValue().getUrl(), Map.Entry::getKey, (a, b) -> a));

    LocalDateTime snapshotAt = LocalDateTime.now();
    AtomicInteger rank = new AtomicInteger(1);

    List<IssueCard> entities =
        rawCards.stream()
            .map(
                card -> {
                  Content content = urlToContent.get(card.sourceUrl());
                  String cardKeyword = urlToKeyword.get(card.sourceUrl());
                  // TODO: 테스트 완료 후 주석 해제
                  // String imageUrl = generateImageSafely(card.imagePrompt());
                  String imageUrl = null;
                  return IssueCard.builder()
                      .title(card.hook())
                      .teaser(card.teaser())
                      .imageUrl(imageUrl)
                      .tags(serializeTags(card.tags()))
                      .category(card.category() != null ? card.category() : "기타")
                      .contentId(content != null ? content.getId() : null)
                      .sourceUrl(content != null ? content.getUrl() : card.sourceUrl())
                      .publishedAt(content != null ? content.getPublishedAt() : null)
                      .snapshotAt(snapshotAt)
                      .rankOrder(rank.getAndIncrement())
                      .keyword(cardKeyword)
                      .build();
                })
            .toList();

    List<IssueCard> savedCards = issueCardRepository.saveAll(entities);
    issueCardRepository.deleteBySnapshotAtBefore(snapshotAt.minusDays(SNAPSHOT_RETENTION_DAYS));

    log.info("키워드 기반 이슈 카드 생성 완료 - {}개", savedCards.size());

    // 각 기사에 대한 비평·신뢰도 분석 사전 계산 (API 호출 시 DB에서 즉시 반환)
    preComputeAnalyses(keywordToContent);
  }

  private void preComputeAnalyses(Map<String, Content> keywordToContent) {
    log.info("기사 분석 사전 계산 시작 - {}개 기사", keywordToContent.size());
    for (Content content : keywordToContent.values()) {
      if (content.getUrl() == null) continue;
      try {
        articleCritiqueService.preComputeAndSave(
            content.getTitle(), content.getBody(), content.getUrl(), content.getId());
      } catch (Exception e) {
        log.error("비평 분석 사전 계산 오류 - url: {}, error: {}", content.getUrl(), e.getMessage());
      }
      try {
        contentAnalysisService.analyze(
            content.getTitle(), content.getUrl(), content.getBody(), null);
      } catch (Exception e) {
        log.error("신뢰도 분석 사전 계산 오류 - url: {}, error: {}", content.getUrl(), e.getMessage());
      }
    }
    log.info("기사 분석 사전 계산 완료");
  }

  private IssueCardResponse toResponse(IssueCard card) {
    return IssueCardResponse.builder()
        .id(card.getContentId() != null ? card.getContentId().toString() : null)
        .title(card.getTitle())
        .teaser(card.getTeaser())
        .imageUrl(card.getImageUrl())
        .timeAgo(toTimeAgo(card.getPublishedAt()))
        .tags(deserializeTags(card.getTags()))
        .category(card.getCategory())
        .url(card.getSourceUrl())
        .build();
  }

  private List<String> buildKeywordList() {
    // 급상승 키워드
    List<String> trending =
        trendingKeywordRepository.findLatest().stream().map(TrendingKeyword::getKeyword).toList();

    // 인기 구독 키워드
    List<String> subscribed;
    try {
      subscribed =
          notificationServiceClient.getPopularKeywords().stream()
              .map(NotificationServiceClient.PopularKeywordInfo::keyword)
              .toList();
    } catch (Exception e) {
      log.warn("구독 키워드 조회 실패 - error: {}", e.getMessage());
      subscribed = List.of();
    }

    // 급상승 키워드 우선, 구독 키워드 중 중복 제거 후 추가
    List<String> result = new java.util.ArrayList<>(trending);
    for (String kw : subscribed) {
      if (!result.contains(kw)) result.add(kw);
    }

    log.info(
        "카드 생성 키워드 - 급상승: {}개, 구독: {}개, 합계: {}개",
        trending.size(),
        subscribed.size(),
        result.size());
    return result;
  }

  private List<Content> findArticleForKeyword(String keyword) {
    // RAG: Qdrant 벡터 검색 우선
    try {
      List<Float> vector = openAiClient.embed(keyword);
      List<Long> ids = qdrantClient.search(vector, 3);
      if (!ids.isEmpty()) {
        Map<Long, Content> contentMap =
            contentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Content::getId, c -> c));
        // Qdrant 유사도 순서 유지 (score 높은 순)
        List<Content> ranked = ids.stream().map(contentMap::get).filter(c -> c != null).toList();
        if (!ranked.isEmpty()) {
          log.debug("Qdrant 검색 성공 - keyword: {}, hits: {}", keyword, ranked.size());
          return List.of(ranked.get(0));
        }
      }
    } catch (Exception e) {
      log.warn("Qdrant 검색 실패, 텍스트 검색으로 폴백 - keyword: {}, error: {}", keyword, e.getMessage());
    }

    // 폴백: 기존 텍스트 검색
    return findArticleByTextMatch(keyword);
  }

  private List<Content> findArticleByTextMatch(String keyword) {
    PageRequest top1 = PageRequest.of(0, 1);
    String[] words = keyword.trim().split("\\s+");

    if (words.length == 1) {
      return contentRepository.findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(keyword, top1);
    }

    List<Content> candidates =
        contentRepository.findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(
            words[0], PageRequest.of(0, 100));

    List<Content> matched =
        candidates.stream().filter(c -> containsAllWords(c.getTitle(), words, 1)).limit(1).toList();

    if (!matched.isEmpty()) return matched;

    return contentRepository.findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(words[0], top1);
  }

  private boolean containsAllWords(String title, String[] words, int fromIndex) {
    String lowerTitle = title.toLowerCase();
    for (int i = fromIndex; i < words.length; i++) {
      if (!lowerTitle.contains(words[i].toLowerCase())) return false;
    }
    return true;
  }

  private String buildKeywordPrompt(Map<String, Content> keywordToContent) {
    StringBuilder sb = new StringBuilder("다음 키워드별로 각각 카드 1개씩 생성해주세요:\n\n");
    int i = 1;
    for (Map.Entry<String, Content> entry : keywordToContent.entrySet()) {
      Content content = entry.getValue();
      sb.append("키워드 ").append(i++).append(": ").append(entry.getKey()).append("\n");
      sb.append("관련 기사: [")
          .append(content.getTitle())
          .append("] (출처: ")
          .append(content.getSource())
          .append(") - URL: ")
          .append(content.getUrl())
          .append("\n\n");
    }
    return sb.toString();
  }

  private List<GptCard> parseGptResponse(String gptResponse) {
    try {
      JsonNode root = objectMapper.readTree(gptResponse);
      JsonNode cardsNode = root.get("cards");
      if (cardsNode == null || !cardsNode.isArray()) {
        log.info("AI 응답 파싱 실패 - 'cards' 필드 없음");
        throw new CustomException(AnalysisErrorCode.AI_PARSE_ERROR);
      }
      return objectMapper.convertValue(cardsNode, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      log.info("AI 응답 JSON 파싱 실패 - error: {}", e.getMessage());
      throw new CustomException(AnalysisErrorCode.AI_PARSE_ERROR);
    }
  }

  private String generateImageSafely(String imagePrompt) {
    if (imagePrompt == null || imagePrompt.isBlank()) {
      return null;
    }
    try {
      byte[] imageBytes = openAiClient.generateImage(imagePrompt);
      return s3ImageService.upload(imageBytes);
    } catch (Exception e) {
      log.error("이미지 생성/업로드 실패 - prompt: {}, error: {}", imagePrompt, e.getMessage(), e);
      return null;
    }
  }

  private String serializeTags(List<String> tags) {
    if (tags == null || tags.isEmpty()) return "[]";
    try {
      return objectMapper.writeValueAsString(tags);
    } catch (JsonProcessingException e) {
      return "[]";
    }
  }

  private List<String> deserializeTags(String tagsJson) {
    if (tagsJson == null || tagsJson.isBlank()) return List.of();
    try {
      return objectMapper.readValue(tagsJson, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      return List.of();
    }
  }

  private String toTimeAgo(LocalDateTime publishedAt) {
    if (publishedAt == null) return "방금 전";
    long minutes = ChronoUnit.MINUTES.between(publishedAt, LocalDateTime.now());
    if (minutes < 1) return "방금 전";
    if (minutes < 60) return minutes + "분 전";
    long hours = minutes / 60;
    if (hours < 24) return hours + "시간 전";
    long days = hours / 24;
    return days + "일 전";
  }

  private record GptCard(
      List<String> tags,
      String hook,
      String teaser,
      String category,
      String sourceUrl,
      String imagePrompt) {}
}
