/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "content_analysis")
public class ContentAnalysis {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 100)
  private String userId;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(length = 2000)
  private String url;

  @Column(name = "total_score", nullable = false)
  private int totalScore;

  @Column(name = "credibility_score", nullable = false)
  private int credibilityScore;

  @Column(name = "accuracy_score", nullable = false)
  private int accuracyScore;

  @Column(name = "bias_score", nullable = false)
  private int biasScore;

  @Column(name = "cross_verification_score", nullable = false)
  private int crossVerificationScore;

  @Column(name = "transparency_score", nullable = false)
  private int transparencyScore;

  @Column(nullable = false, length = 30)
  private String verdict;

  @Column(columnDefinition = "TEXT")
  private String summary;

  // 항목별 근거 + 팩트체크 전체를 JSON으로 저장
  @Column(name = "details_json", columnDefinition = "TEXT")
  private String detailsJson;

  @Column(name = "analyzed_at", nullable = false)
  private LocalDateTime analyzedAt;

  @Column(name = "related_articles_json", columnDefinition = "TEXT")
  private String relatedArticlesJson;

  @Column(name = "stocks_json", columnDefinition = "TEXT")
  private String stocksJson;
}
