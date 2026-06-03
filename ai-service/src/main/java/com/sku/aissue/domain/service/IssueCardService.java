/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import com.sku.aissue.global.s3.S3ImageService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sku.aissue.domain.dto.response.IssueCardResponse;
import com.sku.aissue.domain.entity.IssueCard;
import com.sku.aissue.domain.exception.AnalysisErrorCode;
import com.sku.aissue.domain.repository.IssueCardRepository;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.global.client.ContentServiceClient;
import com.sku.aissue.global.client.ContentServiceClient.ContentInfo;
import com.sku.aissue.global.client.NotificationServiceClient;
import com.sku.aissue.global.client.OpenAiClient;
import com.sku.aissue.global.client.QdrantClient;
import com.sku.aissue.global.messaging.CardEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueCardService {

  private static final int SNAPSHOT_RETENTION_DAYS = 7;
  private static final int TARGET_CARD_COUNT = 10;
  private static final String FILLER_KEY_PREFIX = "__filler_";

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

      category rules:
      - Choose exactly ONE from: 정치, 경제, 사회, 과학/IT, 문화/연예, 스포츠, 국제, 기타

      imagePrompt rules:
      - Describe a scene or concept that visually represents the topic
      - Photorealistic documentary/photojournalism style — like a Reuters or AP news photograph, NOT digital illustration or AI art
      - Natural lighting, realistic textures, candid composition, shallow depth of field
      - Set in South Korea
      - Any people depicted MUST be ethnically Korean (East Asian features, black hair) — unless the topic is explicitly about a foreign country or foreign people
      - Use Korean settings where relevant: Korean cityscape, Korean architecture, Hangul signage in background, Korean street/office/home interiors
      - No text overlays, no watermarks, no specific real persons' faces, no cartoon/illustration aesthetic, no oversaturated colors
      - In English

      Rules:
      - Generate EXACTLY one card per keyword provided — no more, no less
      - sourceUrl must be the exact URL provided for that keyword's article
      - hook must be declarative (no question marks), intense but related to the news
      """;

  private final IssueCardRepository issueCardRepository;
  private final ContentServiceClient contentServiceClient;
  private final NotificationServiceClient notificationServiceClient;
  private final OpenAiClient openAiClient;
  private final QdrantClient qdrantClient;
  private final ObjectMapper objectMapper;
  private final S3ImageService s3ImageService;
  private final ArticleCritiqueService articleCritiqueService;
  private final ContentAnalysisService contentAnalysisService;
  private final StockAnalysisService stockAnalysisService;
  private final CardEventPublisher cardEventPublisher;

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
    if (username == null) return getIssueCards();

    List<String> subscribedKeywords = notificationServiceClient.getUserSubscribedKeywords(username);
    if (subscribedKeywords.isEmpty()) return getIssueCards();

    List<IssueCard> subscribed = issueCardRepository.findLatestByKeywords(subscribedKeywords);
    List<IssueCard> all = issueCardRepository.findLatest();
    Set<Long> subscribedIds = subscribed.stream().map(IssueCard::getId).collect(Collectors.toSet());
    List<IssueCard> merged = new ArrayList<>(subscribed);
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
  public void generateAndSaveByHotTopics(boolean generateImages) {
    log.info("키워드 기반 이슈 카드 생성 시작 - 이미지 생성: {}", generateImages);

    List<String> keywords = buildKeywordList();
    if (keywords.isEmpty()) {
      log.info("처리할 키워드 없음 - 이슈 카드 생성 스킵");
      return;
    }

    Map<String, ContentInfo> keywordToContent = collectKeywordArticles(keywords, TARGET_CARD_COUNT);

    log.info("매칭된 키워드-기사 쌍: {}개 / 전체 {}개", keywordToContent.size(), keywords.size());

    if (keywordToContent.isEmpty()) {
      log.info("키워드에 매칭되는 기사 없음 - 이슈 카드 생성 스킵");
      return;
    }

    String userPrompt = buildKeywordPrompt(keywordToContent);
    String aiResponse = openAiClient.chat(SYSTEM_PROMPT, userPrompt);
    List<GptCard> rawCards = parseGptResponse(aiResponse);
    log.info("GPT 카드 파싱 결과: {}개", rawCards.size());

    Map<String, ContentInfo> urlToContent =
        keywordToContent.values().stream()
            .filter(c -> c.url() != null)
            .collect(Collectors.toMap(ContentInfo::url, c -> c, (a, b) -> a));

    Map<String, String> urlToKeyword =
        keywordToContent.entrySet().stream()
            .filter(e -> e.getValue().url() != null && !e.getKey().startsWith(FILLER_KEY_PREFIX))
            .collect(Collectors.toMap(e -> e.getValue().url(), Map.Entry::getKey, (a, b) -> a));

    LocalDateTime snapshotAt = LocalDateTime.now();
    AtomicInteger rank = new AtomicInteger(1);

    List<IssueCard> entities =
        rawCards.stream()
            .map(
                card -> {
                  ContentInfo content = urlToContent.get(card.sourceUrl());
                  String cardKeyword = urlToKeyword.get(card.sourceUrl());
                  String imageUrl = generateImages ? generateImageSafely(card.imagePrompt()) : null;
                  return IssueCard.builder()
                      .title(card.hook())
                      .teaser(card.teaser())
                      .imageUrl(imageUrl)
                      .tags(serializeTags(card.tags()))
                      .category(card.category() != null ? card.category() : "기타")
                      .contentId(content != null ? content.id() : null)
                      .sourceUrl(content != null ? content.url() : card.sourceUrl())
                      .publishedAt(content != null ? content.publishedAt() : null)
                      .snapshotAt(snapshotAt)
                      .rankOrder(rank.getAndIncrement())
                      .keyword(cardKeyword)
                      .build();
                })
            .toList();

    List<IssueCard> savedCards = issueCardRepository.saveAll(entities);
    issueCardRepository.deleteBySnapshotAtBefore(snapshotAt.minusDays(SNAPSHOT_RETENTION_DAYS));

    log.info("공통 이슈 카드 생성 완료 - {}개", savedCards.size());

    publishCardsGeneratedEvent(savedCards);
    preComputeAnalyses(keywordToContent);
  }

  private void publishCardsGeneratedEvent(List<IssueCard> cards) {
    List<CardEventPublisher.CardInfo> cardInfos =
        cards.stream()
            .filter(c -> c.getSourceUrl() != null)
            .map(
                c ->
                    new CardEventPublisher.CardInfo(
                        c.getTitle(),
                        c.getSourceUrl(),
                        c.getContentId(),
                        deserializeTags(c.getTags())))
            .toList();
    cardEventPublisher.publishCardsGenerated(cardInfos);
  }

  private void preComputeAnalyses(Map<String, ContentInfo> keywordToContent) {
    log.info("기사 분석 사전 계산 시작 - {}개 기사", keywordToContent.size());
    for (ContentInfo content : keywordToContent.values()) {
      if (content.url() == null) continue;
      try {
        articleCritiqueService.preComputeAndSave(
            content.title(), content.body(), content.url(), content.id());
      } catch (Exception e) {
        log.error("비평 분석 사전 계산 오류 - url: {}, error: {}", content.url(), e.getMessage());
      }
      try {
        contentAnalysisService.analyze(content.title(), content.url(), content.body(), null);
      } catch (Exception e) {
        log.error("신뢰도 분석 사전 계산 오류 - url: {}, error: {}", content.url(), e.getMessage());
      }
      try {
        var stocks = stockAnalysisService.analyzeByText(content.title(), content.body());
        contentAnalysisService.saveStocks(content.url(), stocks);
      } catch (Exception e) {
        log.warn("종목 분석 사전 계산 오류 - url: {}, error: {}", content.url(), e.getMessage());
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
    List<String> trending = contentServiceClient.getHotKeywords();
    log.info("카드 생성 키워드 - 급상승: {}개", trending.size());
    return trending;
  }

  private Map<String, ContentInfo> collectKeywordArticles(List<String> keywords, int limit) {
    Map<String, ContentInfo> result = new LinkedHashMap<>();
    Set<String> usedUrls = new HashSet<>();

    for (String keyword : keywords) {
      if (result.size() >= limit) break;
      ContentInfo article = findArticleForKeyword(keyword);
      if (article == null) {
        log.info("키워드 매칭 실패 - keyword: {}", keyword);
        continue;
      }
      if (article.url() != null && !usedUrls.add(article.url())) continue;
      result.put(keyword, article);
      log.info("키워드 매칭 - keyword: {}, title: {}", keyword, article.title());
    }

    if (result.size() < limit) {
      fillWithRecentArticles(result, usedUrls, limit);
    }

    return result;
  }

  private void fillWithRecentArticles(
      Map<String, ContentInfo> result, Set<String> usedUrls, int limit) {
    try {
      ContentServiceClient.PagedContentInfo paged = contentServiceClient.findPaged(0, limit * 3);
      int fillerIdx = 1;
      for (ContentInfo content : paged.contents()) {
        if (result.size() >= limit) break;
        if (content.url() != null && !usedUrls.add(content.url())) continue;
        result.put(FILLER_KEY_PREFIX + fillerIdx++, content);
      }
      log.info("최근 기사로 카드 보충 완료 - 총 {}개", result.size());
    } catch (Exception e) {
      log.warn("최근 기사 조회 실패 - 현재 {}개로 진행: {}", result.size(), e.getMessage());
    }
  }

  private ContentInfo findArticleForKeyword(String keyword) {
    LocalDateTime threshold = LocalDateTime.now().minusDays(3);

    try {
      List<Float> vector = openAiClient.embed(keyword);
      List<Long> ids = qdrantClient.search(vector, 3);
      if (!ids.isEmpty()) {
        List<ContentInfo> contents = contentServiceClient.findByIds(ids);
        Map<Long, ContentInfo> contentMap =
            contents.stream().collect(Collectors.toMap(ContentInfo::id, c -> c));
        ContentInfo ranked =
            ids.stream()
                .map(contentMap::get)
                .filter(c -> c != null)
                .filter(c -> c.publishedAt() == null || c.publishedAt().isAfter(threshold))
                .findFirst()
                .orElse(null);
        if (ranked != null) {
          log.debug("Qdrant 검색 성공 - keyword: {}", keyword);
          return ranked;
        }
      }
    } catch (Exception e) {
      log.warn("Qdrant 검색 실패, 텍스트 검색으로 폴백 - keyword: {}, error: {}", keyword, e.getMessage());
    }

    ContentInfo article = findArticleByTextMatch(keyword);
    if (article != null
        && article.publishedAt() != null
        && article.publishedAt().isBefore(threshold)) {
      log.info(
          "키워드 매칭 기사가 너무 오래됨 ({}일 이상) - keyword: {}, publishedAt: {}",
          3,
          keyword,
          article.publishedAt());
      return null;
    }
    return article;
  }

  private ContentInfo findArticleByTextMatch(String keyword) {
    String[] words = keyword.trim().split("\\s+");

    if (words.length == 1) {
      List<ContentInfo> results = contentServiceClient.findByTitle(keyword, 0, 1);
      return results.isEmpty() ? null : results.get(0);
    }

    List<ContentInfo> candidates = contentServiceClient.findByTitle(words[0], 0, 100);

    ContentInfo matched =
        candidates.stream()
            .filter(c -> containsAllWords(c.title(), words, 1))
            .findFirst()
            .orElse(null);

    if (matched != null) return matched;

    List<ContentInfo> fallback = contentServiceClient.findByTitle(words[0], 0, 1);
    return fallback.isEmpty() ? null : fallback.get(0);
  }

  private boolean containsAllWords(String title, String[] words, int fromIndex) {
    if (title == null) return false;
    String lowerTitle = title.toLowerCase();
    for (int i = fromIndex; i < words.length; i++) {
      if (!lowerTitle.contains(words[i].toLowerCase())) return false;
    }
    return true;
  }

  private String buildKeywordPrompt(Map<String, ContentInfo> keywordToContent) {
    StringBuilder sb = new StringBuilder("다음 키워드별로 각각 카드 1개씩 생성해주세요:\n\n");
    int i = 1;
    for (Map.Entry<String, ContentInfo> entry : keywordToContent.entrySet()) {
      ContentInfo content = entry.getValue();
      String keyword =
          entry.getKey().startsWith(FILLER_KEY_PREFIX)
              ? extractFirstKeyword(content.title())
              : entry.getKey();
      sb.append("키워드 ")
          .append(i++)
          .append(": ")
          .append(keyword != null ? keyword : "최신뉴스")
          .append("\n");
      sb.append("관련 기사: [")
          .append(content.title())
          .append("] (출처: ")
          .append(content.source())
          .append(") - URL: ")
          .append(content.url())
          .append("\n\n");
    }
    return sb.toString();
  }

  private String extractFirstKeyword(String title) {
    if (title == null || title.isBlank()) return null;
    return Arrays.stream(title.split("[^가-힣a-zA-Z0-9]+"))
        .filter(w -> w.length() >= 2)
        .findFirst()
        .orElse(null);
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

  private static final String IMAGE_PROMPT_PREFIX =
      "Photorealistic documentary news photograph, shot on a 35mm DSLR with natural lighting and shallow depth of field, "
          + "in the style of Reuters or AP photojournalism. Candid composition, realistic skin texture, real-world imperfections. "
          + "Set in South Korea. "
          + "All people depicted are ethnically Korean with East Asian features and black hair "
          + "(unless the subject is explicitly about a foreign country or foreign people). "
          + "Korean cultural context: Korean cityscape, Korean architecture, Hangul signage in background where natural. "
          + "Avoid: digital illustration, cartoon style, anime, 3D render, oversaturated colors, overly smooth skin, AI-art aesthetic, text overlays, watermarks, specific real persons' faces. "
          + "Subject: ";

  private String generateImageSafely(String imagePrompt) {
    if (imagePrompt == null || imagePrompt.isBlank()) {
      return null;
    }
    String wrappedPrompt = IMAGE_PROMPT_PREFIX + imagePrompt;
    try {
      byte[] imageBytes = openAiClient.generateImage(wrappedPrompt);
      return s3ImageService.upload(imageBytes);
    } catch (Exception e) {
      log.error("이미지 생성/업로드 실패 - prompt: {}, error: {}", wrappedPrompt, e.getMessage(), e);
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
