/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.CollectedContentDto;
import com.sku.aissue.domain.entity.Content;
import com.sku.aissue.domain.entity.ContentSource;
import com.sku.aissue.domain.entity.ContentType;
import com.sku.aissue.domain.repository.ContentRepository;
import com.sku.aissue.domain.repository.TrendingKeywordRepository;
import com.sku.aissue.domain.service.ContentSaveService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class InternalContentControllerImpl implements InternalContentController {

  private final ContentRepository contentRepository;
  private final ContentSaveService contentSaveService;
  private final TrendingKeywordRepository trendingKeywordRepository;

  @Override
  public ResponseEntity<List<String>> getHotKeywords() {
    List<String> keywords =
        trendingKeywordRepository.findLatest().stream().map(tk -> tk.getKeyword()).toList();
    log.info("내부 API - 급상승 키워드 조회: {}개", keywords.size());
    return ResponseEntity.ok(keywords);
  }

  @Override
  public ResponseEntity<ContentInfo> getById(Long id) {
    return contentRepository
        .findById(id)
        .map(c -> ResponseEntity.ok(toInfo(c)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<List<ContentInfo>> getByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) return ResponseEntity.ok(List.of());
    List<ContentInfo> result =
        contentRepository.findAllById(ids).stream().map(this::toInfo).toList();
    return ResponseEntity.ok(result);
  }

  @Override
  public ResponseEntity<List<ContentInfo>> getByTitle(String keyword, int page, int size) {
    List<ContentInfo> result =
        contentRepository
            .findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(
                keyword, PageRequest.of(page, size))
            .stream()
            .map(this::toInfo)
            .toList();
    return ResponseEntity.ok(result);
  }

  @Override
  public ResponseEntity<PagedContentInfo> getPaged(int page, int size) {
    Page<Content> fetched =
        contentRepository.findAll(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt")));
    List<ContentInfo> contents = fetched.getContent().stream().map(this::toInfo).toList();
    return ResponseEntity.ok(
        new PagedContentInfo(contents, fetched.hasNext(), fetched.getTotalPages()));
  }

  @Override
  public ResponseEntity<ContentInfo> saveOrFind(SaveContentRequest request) {
    CollectedContentDto dto =
        CollectedContentDto.builder()
            .title(request.title())
            .body(request.body())
            .url(request.url())
            .source(request.source())
            .contentType(parseEnum(ContentType.class, request.contentType(), ContentType.ISSUE))
            .contentSource(
                parseEnum(
                    ContentSource.class, request.contentSource(), ContentSource.USER_SUBMITTED))
            .publishedAt(java.time.LocalDateTime.now())
            .build();
    Content saved = contentSaveService.findOrSave(dto);
    return ResponseEntity.ok(toInfo(saved));
  }

  @Override
  public ResponseEntity<Long> getIdByUrl(String url) {
    return contentRepository
        .findFirstByUrl(url)
        .map(c -> ResponseEntity.ok(c.getId()))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private ContentInfo toInfo(Content c) {
    return new ContentInfo(
        c.getId(), c.getTitle(), c.getBody(), c.getUrl(), c.getSource(), c.getPublishedAt());
  }

  private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value, E defaultValue) {
    if (value == null) return defaultValue;
    try {
      return Enum.valueOf(clazz, value);
    } catch (IllegalArgumentException e) {
      return defaultValue;
    }
  }
}
