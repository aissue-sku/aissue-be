/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sku.aissue.domain.collector.ContentCollector;
import com.sku.aissue.domain.dto.CollectedContentDto;
import com.sku.aissue.domain.entity.Content;
import com.sku.aissue.domain.service.ArticleEmbeddingService;
import com.sku.aissue.domain.service.ContentSaveService;
import com.sku.aissue.domain.service.HotTopicService;
import com.sku.aissue.domain.service.IssueCardService;
import com.sku.aissue.domain.service.TrendingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionScheduler {

  private final List<ContentCollector> collectors;
  private final ContentSaveService contentSaveService;
  private final TrendingService trendingService;
  private final IssueCardService issueCardService;
  private final HotTopicService hotTopicService;
  private final ArticleEmbeddingService articleEmbeddingService;

  // 30분마다 뉴스 수집
  @Scheduled(cron = "${scheduler.news.cron}")
  public void collectNews() {
    log.info("뉴스 수집 스케줄러 시작");

    collectors.forEach(
        collector -> {
          log.info("수집 시작 - source: {}", collector.getSource());
          List<CollectedContentDto> collected = collector.collect();
          List<Content> saved = contentSaveService.saveAll(collected);
          log.info(
              "수집 완료 - source: {}, 수집: {}건, 저장: {}건",
              collector.getSource(),
              collected.size(),
              saved.size());
          if (!saved.isEmpty()) {
            articleEmbeddingService.embedBatch(saved);
          }
        });
  }

  // 매 시간 트렌딩 갱신
  @Scheduled(cron = "${scheduler.trending.cron}")
  public void refreshTrending() {
    log.info("트렌딩 스냅샷 갱신 스케줄러 시작");
    trendingService.refreshHourlyTrending();
  }

  // 매일 자정 일간 트렌딩 갱신 + 오래된 스냅샷 정리
  @Scheduled(cron = "0 0 0 * * *")
  public void dailyTask() {
    log.info("일간 트렌딩 갱신 및 스냅샷 정리 시작");
    trendingService.refreshDailyTrending();
    trendingService.deleteOldSnapshots();
  }

  // 00시, 06시, 12시, 18시 — 급상승 키워드 분석 후 키워드 기반 이슈 카드 생성
  // refreshHourlyTrending은 매 시간 refreshTrending()에서 이미 실행됨
  @Scheduled(cron = "${scheduler.hot-topics.cron}")
  public void refreshHotTopics() {
    log.info("급상승 키워드 및 이슈 카드 스케줄러 시작");
    hotTopicService.saveSnapshot();
    issueCardService.generateAndSaveByHotTopics();
  }
}
