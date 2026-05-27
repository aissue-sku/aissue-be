/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sku.aissue.domain.dto.CollectedContentDto;
import com.sku.aissue.domain.dto.request.ContentSubmitRequest;
import com.sku.aissue.domain.dto.response.AnalysisScoreResponse;
import com.sku.aissue.domain.dto.response.ContentResponse;
import com.sku.aissue.domain.dto.response.NewsItemResponse;
import com.sku.aissue.domain.dto.response.TrendingContentResponse;
import com.sku.aissue.domain.entity.Content;
import com.sku.aissue.domain.entity.ContentSource;
import com.sku.aissue.domain.entity.ContentType;
import com.sku.aissue.domain.entity.PeriodType;
import com.sku.aissue.domain.repository.ContentRepository;
import com.sku.aissue.domain.repository.TrendingSnapshotRepository;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.exception.GlobalErrorCode;
import com.sku.aissue.global.client.NotificationServiceClient;
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
  private final ContentSaveService contentSaveService;
  private final ArticleExtractor articleExtractor;
  private final ContentAnalysisService contentAnalysisService;
  private final NotificationServiceClient notificationServiceClient;

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
    // size+1 개 조회로 hasNext 판단
    List<Content> fetched =
        contentRepository.findByKeywordBeforeCursor(
            keyword, effectiveCursor, PageRequest.of(0, size + 1));

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

  @Transactional
  public AnalysisScoreResponse submit(ContentSubmitRequest request, String username) {
    log.info("콘텐츠 분석 요청 시작 - username: {}, type: {}", username, request.getType());
    CollectedContentDto dto = buildSubmittedContent(request, username);
    Content content = contentSaveService.findOrSave(dto);

    AnalysisScoreResponse result =
        contentAnalysisService.analyze(dto.getTitle(), dto.getUrl(), dto.getBody(), username);
    log.info("콘텐츠 분석 성공 - username: {}, totalScore: {}", username, result.getTotalScore());

    notificationServiceClient.sendDirect(username, dto.getTitle(), dto.getUrl(), content.getId());

    return result.toBuilder().contentId(content.getId()).build();
  }

  private CollectedContentDto buildSubmittedContent(ContentSubmitRequest request, String username) {
    if (request.getType() == ContentSubmitRequest.SubmitType.URL) {
      return buildFromUrl(request.getContent(), username);
    }
    return buildFromText(request.getContent(), username);
  }

  private CollectedContentDto buildFromUrl(String url, String username) {
    ArticleExtractor.ExtractedArticle article =
        articleExtractor.extract(url); // 실패 시 CustomException 전파

    String title = (article.title() != null && !article.title().isBlank()) ? article.title() : url;
    String body = article.body();
    String source = username != null ? username : "anonymous";

    return CollectedContentDto.builder()
        .title(title)
        .body(body)
        .url(url)
        .source(source)
        .contentType(ContentType.ISSUE)
        .contentSource(ContentSource.USER_SUBMITTED)
        .publishedAt(java.time.LocalDateTime.now())
        .build();
  }

  private CollectedContentDto buildFromText(String text, String username) {
    String title = text.length() > 50 ? text.substring(0, 50) + "..." : text;
    String source = username != null ? username : "anonymous";

    return CollectedContentDto.builder()
        .title(title)
        .body(text)
        .source(source)
        .contentType(ContentType.ISSUE)
        .contentSource(ContentSource.USER_SUBMITTED)
        .publishedAt(java.time.LocalDateTime.now())
        .build();
  }
}
