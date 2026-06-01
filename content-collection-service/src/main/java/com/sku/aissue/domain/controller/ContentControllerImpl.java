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

import com.sku.aissue.domain.dto.response.ContentResponse;
import com.sku.aissue.domain.dto.response.HotTopicResponse;
import com.sku.aissue.domain.dto.response.NewsItemResponse;
import com.sku.aissue.domain.dto.response.PopularKeywordResponse;
import com.sku.aissue.domain.dto.response.TrendingContentResponse;
import com.sku.aissue.domain.dto.response.TrendingKeywordResponse;
import com.sku.aissue.domain.entity.PeriodType;
import com.sku.aissue.domain.entity.Subscription;
import com.sku.aissue.domain.entity.TrendingKeyword;
import com.sku.aissue.domain.repository.SubscriptionRepository;
import com.sku.aissue.domain.repository.TrendingKeywordRepository;
import com.sku.aissue.domain.service.ContentQueryService;
import com.sku.aissue.domain.service.HotTopicService;
import com.sku.aissue.domain.service.TrendingService;
import com.sku.aissue.global.client.AiServiceClient;
import com.sku.aissue.global.client.NotificationServiceClient;
import com.sku.aissue.global.page.InfiniteResponse;
import com.sku.aissue.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ContentControllerImpl implements ContentController {

  private final ContentQueryService contentQueryService;
  private final TrendingService trendingService;
  private final HotTopicService hotTopicService;
  private final TrendingKeywordRepository trendingKeywordRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final NotificationServiceClient notificationServiceClient;
  private final AiServiceClient aiServiceClient;

  @Override
  public ResponseEntity<BaseResponse<Void>> refreshTrending() {
    trendingService.refreshHourlyTrending();
    trendingService.refreshDailyTrending();
    return ResponseEntity.ok(BaseResponse.success(null));
  }

  @Override
  public ResponseEntity<BaseResponse<List<HotTopicResponse>>> refreshHotTopics() {
    List<HotTopicResponse> keywords = hotTopicService.saveSnapshot();
    aiServiceClient.triggerIssueCards();
    return ResponseEntity.ok(BaseResponse.success(200, "급상승 키워드 갱신 완료", keywords));
  }

  @Override
  public ResponseEntity<BaseResponse<List<TrendingContentResponse>>> getTrending(
      PeriodType periodType) {
    return ResponseEntity.ok(BaseResponse.success(contentQueryService.getTrending(periodType)));
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
  public ResponseEntity<BaseResponse<InfiniteResponse<NewsItemResponse>>> getKeywordNews(
      String keyword, Long cursor, int size) {
    return ResponseEntity.ok(
        BaseResponse.success(contentQueryService.getKeywordNews(keyword, cursor, size)));
  }

  @Override
  public ResponseEntity<BaseResponse<List<PopularKeywordResponse>>> getPopularKeywords() {
    List<PopularKeywordResponse> result =
        notificationServiceClient.getPopularKeywords().stream()
            .map(
                info ->
                    PopularKeywordResponse.builder()
                        .keyword(info.keyword())
                        .subscriberCount(info.subscriberCount())
                        .build())
            .toList();
    return ResponseEntity.ok(BaseResponse.success(result));
  }

  @Override
  public ResponseEntity<BaseResponse<List<TrendingKeywordResponse>>> getHotTopics(
      @AuthenticationPrincipal String username) {
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
