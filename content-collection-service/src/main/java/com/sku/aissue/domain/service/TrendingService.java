/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sku.aissue.domain.entity.Content;
import com.sku.aissue.domain.entity.PeriodType;
import com.sku.aissue.domain.entity.TrendingSnapshot;
import com.sku.aissue.domain.repository.ContentRepository;
import com.sku.aissue.domain.repository.TrendingSnapshotRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TrendingService {

  private static final int TRENDING_SIZE = 30;
  private static final int SNAPSHOT_RETENTION_DAYS = 7;

  private final ContentRepository contentRepository;
  private final TrendingSnapshotRepository trendingSnapshotRepository;
  private final SubscriptionService subscriptionService;

  public TrendingService(
      ContentRepository contentRepository,
      TrendingSnapshotRepository trendingSnapshotRepository,
      SubscriptionService subscriptionService) {
    this.contentRepository = contentRepository;
    this.trendingSnapshotRepository = trendingSnapshotRepository;
    this.subscriptionService = subscriptionService;
  }

  @Transactional
  public void refreshHourlyTrending() {
    log.info("시간별 트렌딩 갱신 요청 시작");
    refresh(PeriodType.HOURLY, LocalDateTime.now().minusHours(1));
    log.info("시간별 트렌딩 갱신 성공");
  }

  @Transactional
  public void refreshDailyTrending() {
    log.info("일별 트렌딩 갱신 요청 시작");
    refresh(PeriodType.DAILY, LocalDateTime.now().minusDays(1));
    log.info("일별 트렌딩 갱신 성공");
  }

  // 보관 기간 초과 스냅샷 정리
  @Transactional
  public void deleteOldSnapshots() {
    log.info("오래된 트렌딩 스냅샷 삭제 요청 시작");
    LocalDateTime threshold = LocalDateTime.now().minusDays(SNAPSHOT_RETENTION_DAYS);
    trendingSnapshotRepository.deleteBySnapshotAtBefore(threshold);
    log.info("오래된 트렌딩 스냅샷 삭제 완료 - 기준: {}", threshold);
  }

  private void refresh(PeriodType periodType, LocalDateTime since) {
    List<Content> recentContents =
        contentRepository.findByCollectedAtAfterOrderByCollectedAtDesc(
            since, PageRequest.of(0, TRENDING_SIZE));

    if (recentContents.isEmpty()) {
      log.info("트렌딩 갱신 스킵 - 최근 수집된 콘텐츠 없음 (periodType: {})", periodType);
      return;
    }

    LocalDateTime snapshotAt = LocalDateTime.now();
    AtomicInteger rankCounter = new AtomicInteger(1);

    List<TrendingSnapshot> snapshots =
        recentContents.stream()
            .map(
                content ->
                    TrendingSnapshot.builder()
                        .content(content)
                        .rankOrder(rankCounter.getAndIncrement())
                        .periodType(periodType)
                        .snapshotAt(snapshotAt)
                        .build())
            .toList();

    trendingSnapshotRepository.saveAll(snapshots);
    log.info("트렌딩 스냅샷 갱신 완료 - periodType: {}, 건수: {}", periodType, snapshots.size());
    subscriptionService.createNotificationsFromSnapshots(snapshots);
  }
}
