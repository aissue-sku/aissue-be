/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sku.aissue.domain.entity.Content;

public interface ContentRepository extends JpaRepository<Content, Long> {

  // 중복 수집 방지: URL 해시 존재 여부 확인
  boolean existsByUrlHash(String urlHash);

  Optional<Content> findByUrlHash(String urlHash);

  // 트렌딩 갱신용: 특정 시각 이후 수집된 콘텐츠를 최신순으로 조회
  List<Content> findByCollectedAtAfterOrderByCollectedAtDesc(
      LocalDateTime after, Pageable pageable);

  // 키워드 검색 (title 기준)
  List<Content> findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(String keyword);

  // 키워드 검색 - 건수 제한
  List<Content> findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(
      String keyword, Pageable pageable);

  // 바이그램 키워드용: 두 단어 모두 포함하는 기사 검색
  @Query(
      "SELECT c FROM Content c WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :word1, '%')) AND LOWER(c.title) LIKE LOWER(CONCAT('%', :word2, '%')) ORDER BY c.publishedAt DESC")
  List<Content> findByTitleContainingBothWords(
      @Param("word1") String word1, @Param("word2") String word2, Pageable pageable);

  // 이미지 미생성 기사 조회 (배치 처리용)
  List<Content> findByImageUrlIsNullOrderByCollectedAtDesc(Pageable pageable);
}
