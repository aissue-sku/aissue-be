/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sku.aissue.common.BaseTimeEntity;

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
@Table(name = "content")
public class Content extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String body;

  @Column(length = 2000)
  private String url;

  // 중복 수집 방지용 URL SHA-256 해시
  @Column(name = "url_hash", length = 64, unique = true)
  private String urlHash;

  // 언론사, RSS 피드명 등 출처명
  @Column(nullable = false, length = 100)
  private String source;

  @Column(length = 50)
  private String category;

  @Column(name = "content_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private ContentType contentType;

  @Column(name = "content_source", nullable = false)
  @Enumerated(EnumType.STRING)
  private ContentSource contentSource;

  // 원본 기사 발행 시각
  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  // 이 서비스가 수집한 시각
  @Column(name = "collected_at", nullable = false)
  private LocalDateTime collectedAt;

  @Column(name = "image_url", length = 2000)
  private String imageUrl;

  public void updateImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }
}
