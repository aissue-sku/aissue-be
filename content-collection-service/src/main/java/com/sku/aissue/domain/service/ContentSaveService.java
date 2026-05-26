/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sku.aissue.domain.dto.CollectedContentDto;
import com.sku.aissue.domain.entity.Content;
import com.sku.aissue.domain.repository.ContentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSaveService {

  private final ContentRepository contentRepository;

  @Transactional
  public Content findOrSave(CollectedContentDto dto) {
    String hashTarget = dto.getUrl() != null ? dto.getUrl() : dto.getTitle();
    String urlHash = sha256(hashTarget);
    return contentRepository
        .findByUrlHash(urlHash)
        .orElseGet(() -> contentRepository.save(toEntity(dto, urlHash)));
  }

  @Transactional
  public int saveAll(List<CollectedContentDto> dtos) {
    log.info("콘텐츠 저장 요청 시작 - 전체: {}건", dtos.size());
    int savedCount = 0;

    for (CollectedContentDto dto : dtos) {
      try {
        savedCount += saveIfNotDuplicate(dto);
      } catch (Exception e) {
        log.info("콘텐츠 저장 실패 - title: {}, error: {}", dto.getTitle(), e.getMessage());
      }
    }

    log.info("콘텐츠 저장 완료 - 전체: {}건, 신규 저장: {}건", dtos.size(), savedCount);
    return savedCount;
  }

  private int saveIfNotDuplicate(CollectedContentDto dto) {
    // URL이 없는 경우 제목으로 해시
    String hashTarget = dto.getUrl() != null ? dto.getUrl() : dto.getTitle();
    String urlHash = sha256(hashTarget);

    if (contentRepository.existsByUrlHash(urlHash)) {
      return 0;
    }

    contentRepository.save(toEntity(dto, urlHash));
    return 1;
  }

  private Content toEntity(CollectedContentDto dto, String urlHash) {
    return Content.builder()
        .title(dto.getTitle())
        .body(dto.getBody())
        .url(dto.getUrl())
        .urlHash(urlHash)
        .source(dto.getSource())
        .category(dto.getCategory())
        .contentType(dto.getContentType())
        .contentSource(dto.getContentSource())
        .publishedAt(dto.getPublishedAt())
        .collectedAt(LocalDateTime.now())
        .build();
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 해싱 실패", e);
    }
  }
}
