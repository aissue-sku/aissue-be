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
import jakarta.persistence.Index;
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
@Table(
    name = "article_critique",
    indexes = {@Index(columnList = "url")})
public class ArticleCritique {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 768)
  private String url;

  @Column(name = "content_id")
  private Long contentId;

  // GPT 원본 응답 JSON (parseResponse()로 재구성)
  @Column(name = "result_json", columnDefinition = "TEXT")
  private String resultJson;

  @Column(name = "analyzed_at", nullable = false)
  private LocalDateTime analyzedAt;
}
