/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.request.ContentSubmitRequest;
import com.sku.aissue.domain.dto.response.AnalysisHistoryResponse;
import com.sku.aissue.domain.dto.response.AnalysisScoreResponse;
import com.sku.aissue.domain.dto.response.ArticleCritiqueResponse;
import com.sku.aissue.domain.dto.response.ContentResponse;
import com.sku.aissue.domain.dto.response.HotTopicResponse;
import com.sku.aissue.domain.dto.response.NewsItemResponse;
import com.sku.aissue.domain.dto.response.TrendingContentResponse;
import com.sku.aissue.domain.dto.response.TrendingKeywordResponse;
import com.sku.aissue.domain.entity.PeriodType;
import com.sku.aissue.domain.entity.Subscription;
import com.sku.aissue.domain.entity.TrendingKeyword;
import com.sku.aissue.domain.repository.ContentAnalysisRepository;
import com.sku.aissue.domain.repository.SubscriptionRepository;
import com.sku.aissue.domain.repository.TrendingKeywordRepository;
import com.sku.aissue.domain.service.ArticleCritiqueService;
import com.sku.aissue.domain.service.ArticleExtractor;
import com.sku.aissue.domain.service.ContentAnalysisService;
import com.sku.aissue.domain.service.ContentQueryService;
import com.sku.aissue.domain.service.HotTopicService;
import com.sku.aissue.domain.service.IssueCardService;
import com.sku.aissue.domain.service.TrendingService;
import com.sku.aissue.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ContentControllerImpl implements ContentController {

  private final ContentQueryService contentQueryService;
  private final TrendingService trendingService;
  private final IssueCardService issueCardService;
  private final HotTopicService hotTopicService;
  private final ArticleCritiqueService articleCritiqueService;
  private final ContentAnalysisService contentAnalysisService;
  private final ArticleExtractor articleExtractor;
  private final TrendingKeywordRepository trendingKeywordRepository;
  private final ContentAnalysisRepository contentAnalysisRepository;
  private final SubscriptionRepository subscriptionRepository;

  @Override
  public ResponseEntity<BaseResponse<Void>> refreshTrending() {
    trendingService.refreshHourlyTrending();
    trendingService.refreshDailyTrending();
    return ResponseEntity.ok(BaseResponse.success(null));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> refreshIssueCards() {
    issueCardService.generateAndSaveByHotTopics();
    return ResponseEntity.ok(BaseResponse.success(null));
  }

  @Override
  public ResponseEntity<BaseResponse<List<HotTopicResponse>>> refreshHotTopics() {
    List<HotTopicResponse> keywords = hotTopicService.saveSnapshot();
    issueCardService.generateAndSaveByHotTopics();
    return ResponseEntity.ok(BaseResponse.success(200, "급상승 키워드 갱신 완료", keywords));
  }

  @Override
  public ResponseEntity<BaseResponse<List<TrendingContentResponse>>> getTrending(
      PeriodType periodType) {
    return ResponseEntity.ok(BaseResponse.success(contentQueryService.getTrending(periodType)));
  }

  @Override
  public ResponseEntity<BaseResponse<ArticleCritiqueResponse>> getCritiqueByContentId(Long id) {
    return ResponseEntity.ok(BaseResponse.success(articleCritiqueService.getByContentId(id)));
  }

  @Override
  public ResponseEntity<BaseResponse<AnalysisScoreResponse>> getAnalysisByContentId(Long id) {
    return ResponseEntity.ok(BaseResponse.success(contentAnalysisService.getByContentId(id)));
  }

  @Override
  public ResponseEntity<BaseResponse<ContentResponse>> getById(Long id) {
    return ResponseEntity.ok(BaseResponse.success(contentQueryService.getById(id)));
  }

  @Override
  public ResponseEntity<BaseResponse<List<ContentResponse>>> search(String keyword) {
    return ResponseEntity.ok(BaseResponse.success(contentQueryService.search(keyword)));
  }

  @Override
  public ResponseEntity<BaseResponse<AnalysisScoreResponse>> submit(
      ContentSubmitRequest request, @AuthenticationPrincipal String username) {
    AnalysisScoreResponse result = contentQueryService.submit(request, username);
    return ResponseEntity.ok(BaseResponse.success(result));
  }

  @Override
  public ResponseEntity<BaseResponse<ArticleCritiqueResponse>> critique(
      ContentSubmitRequest request, @AuthenticationPrincipal String username) {
    String title;
    String body;

    ArticleCritiqueResponse result;
    if (request.getType() == ContentSubmitRequest.SubmitType.URL) {
      String url = request.getContent();
      ArticleExtractor.ExtractedArticle article = articleExtractor.extract(url);
      title = (article.title() != null && !article.title().isBlank()) ? article.title() : url;
      body = article.body();
      result = articleCritiqueService.analyzeWithCache(title, body, url);
    } else {
      String text = request.getContent();
      title = text.length() > 50 ? text.substring(0, 50) + "..." : text;
      body = text;
      result = articleCritiqueService.analyze(title, body);
    }

    return ResponseEntity.ok(BaseResponse.success(result));
  }

  @Override
  public ResponseEntity<BaseResponse<List<AnalysisHistoryResponse>>> getAnalysisHistory(
      @AuthenticationPrincipal String username) {
    List<AnalysisHistoryResponse> history =
        contentAnalysisRepository.findByUserIdOrderByAnalyzedAtDesc(username).stream()
            .map(AnalysisHistoryResponse::from)
            .toList();
    return ResponseEntity.ok(BaseResponse.success(history));
  }

  @Override
  public ResponseEntity<BaseResponse<List<NewsItemResponse>>> getKeywordNews(
      String keyword, int page, int size) {
    return ResponseEntity.ok(
        BaseResponse.success(contentQueryService.getKeywordNews(keyword, page, size)));
  }

  @Override
  public ResponseEntity<BaseResponse<List<TrendingKeywordResponse>>> getHotTopics(String username) {
    List<TrendingKeyword> raw = trendingKeywordRepository.findLatest();

    Set<String> subscribedKeywords =
        username != null
            ? subscriptionRepository.findByUserId(username).stream()
                .map(Subscription::getKeyword)
                .collect(Collectors.toSet())
            : Set.of();

    List<TrendingKeywordResponse> result = new ArrayList<>();
    for (int i = 0; i < raw.size(); i++) {
      TrendingKeyword tk = raw.get(i);
      int rank = i + 1;
      boolean hot = tk.getSurgeRatio() >= 3.0 || (tk.getSurgeRatio() == 0.0 && rank <= 3);
      result.add(
          TrendingKeywordResponse.builder()
              .rank(rank)
              .keyword(tk.getKeyword())
              .count(tk.getCount())
              .hot(hot)
              .subscribed(subscribedKeywords.contains(tk.getKeyword()))
              .build());
    }
    return ResponseEntity.ok(BaseResponse.success(result));
  }
}
