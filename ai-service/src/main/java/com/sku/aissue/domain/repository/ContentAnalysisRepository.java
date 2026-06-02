/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sku.aissue.domain.entity.ContentAnalysis;

public interface ContentAnalysisRepository extends JpaRepository<ContentAnalysis, Long> {

  List<ContentAnalysis> findByUserIdOrderByAnalyzedAtDesc(String userId);

  Optional<ContentAnalysis> findTopByUrlOrderByAnalyzedAtDesc(String url);

  boolean existsByUserIdAndUrl(String userId, String url);

  @Modifying
  @Query(
      value =
          "UPDATE content_analysis SET related_articles_json = :relatedJson WHERE url = :url AND analyzed_at = (SELECT max_at FROM (SELECT MAX(analyzed_at) AS max_at FROM content_analysis WHERE url = :url) AS sub)",
      nativeQuery = true)
  int updateRelatedArticles(@Param("url") String url, @Param("relatedJson") String relatedJson);

  @Modifying
  @Query(
      value =
          "UPDATE content_analysis SET stocks_json = :stocksJson WHERE url = :url AND analyzed_at = (SELECT max_at FROM (SELECT MAX(analyzed_at) AS max_at FROM content_analysis WHERE url = :url) AS sub)",
      nativeQuery = true)
  int updateStocks(@Param("url") String url, @Param("stocksJson") String stocksJson);
}
