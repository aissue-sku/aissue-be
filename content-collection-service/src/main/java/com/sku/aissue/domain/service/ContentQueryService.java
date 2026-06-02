/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sku.aissue.domain.dto.response.ContentResponse;
import com.sku.aissue.domain.dto.response.NewsItemResponse;
import com.sku.aissue.domain.dto.response.TrendingContentResponse;
import com.sku.aissue.domain.entity.Content;
import com.sku.aissue.domain.entity.PeriodType;
import com.sku.aissue.domain.repository.ContentRepository;
import com.sku.aissue.domain.repository.TrendingSnapshotRepository;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.exception.GlobalErrorCode;
import com.sku.aissue.global.page.InfiniteResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentQueryService {

  private final ContentRepository contentRepository;
  private final TrendingSnapshotRepository trendingSnapshotRepository;

  public List<TrendingContentResponse> getTrending(PeriodType periodType) {
    log.info("트렌딩 조회 요청 시작 - periodType: {}", periodType);
    List<TrendingContentResponse> result =
        trendingSnapshotRepository.findLatestByPeriodType(periodType).stream()
            .map(TrendingContentResponse::from)
            .toList();
    log.info("트렌딩 조회 성공 - periodType: {}, 건수: {}", periodType, result.size());
    return result;
  }

  public ContentResponse getById(Long id) {
    log.info("콘텐츠 단건 조회 요청 시작 - id: {}", id);
    return contentRepository
        .findById(id)
        .map(
            content -> {
              log.info("콘텐츠 단건 조회 성공 - id: {}", id);
              return ContentResponse.from(content);
            })
        .orElseThrow(
            () -> {
              log.info("콘텐츠 단건 조회 실패 - 존재하지 않는 id: {}", id);
              return new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
            });
  }

  public List<ContentResponse> search(String keyword) {
    log.info("콘텐츠 검색 요청 시작 - keyword: {}", keyword);
    List<ContentResponse> result =
        contentRepository.findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(keyword).stream()
            .map(ContentResponse::from)
            .toList();
    log.info("콘텐츠 검색 성공 - keyword: {}, 건수: {}", keyword, result.size());
    return result;
  }

  public InfiniteResponse<NewsItemResponse> getKeywordNews(String keyword, Long cursor, int size) {
    log.info("키워드 뉴스 조회 - keyword: {}, cursor: {}, size: {}", keyword, cursor, size);

    long effectiveCursor = cursor != null ? cursor : Long.MAX_VALUE;
    String[] words = keyword.trim().split("\\s+");

    List<Content> fetched;
    if (words.length == 1) {
      fetched =
          contentRepository.findByKeywordBeforeCursor(
              keyword, effectiveCursor, PageRequest.of(0, size + 1));
    } else {
      int fetchSize = Math.max(size * 10, 100);
      Map<Long, Content> candidateMap = new LinkedHashMap<>();
      Map<Long, Integer> matchCount = new HashMap<>();

      for (String word : words) {
        contentRepository
            .findByKeywordBeforeCursor(word, effectiveCursor, PageRequest.of(0, fetchSize))
            .forEach(
                c -> {
                  candidateMap.putIfAbsent(c.getId(), c);
                  matchCount.merge(c.getId(), 1, Integer::sum);
                });
      }

      fetched = new ArrayList<>(candidateMap.values());
      fetched.sort(
          Comparator.comparingInt((Content c) -> matchCount.getOrDefault(c.getId(), 0))
              .reversed()
              .thenComparingLong(Content::getId)
              .reversed());
      if (fetched.size() > size + 1) fetched = fetched.subList(0, size + 1);
    }

    boolean hasNext = fetched.size() > size;
    List<Content> pageContents = hasNext ? fetched.subList(0, size) : fetched;

    List<NewsItemResponse> content =
        pageContents.stream()
            .map(
                c ->
                    NewsItemResponse.builder()
                        .id(c.getId().toString())
                        .title(c.getTitle())
                        .imageUrl(c.getImageUrl())
                        .timeAgo(
                            toTimeAgo(
                                c.getPublishedAt() != null
                                    ? c.getPublishedAt()
                                    : c.getCollectedAt()))
                        .url(c.getUrl())
                        .build())
            .toList();

    Long lastCursor = content.isEmpty() ? null : Long.parseLong(content.getLast().getId());
    log.info(
        "키워드 뉴스 조회 성공 - keyword: {}, 반환 건수: {}, hasNext: {}", keyword, content.size(), hasNext);
    return InfiniteResponse.<NewsItemResponse>builder()
        .content(content)
        .lastCursor(lastCursor)
        .hasNext(hasNext)
        .size(content.size())
        .build();
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
}
