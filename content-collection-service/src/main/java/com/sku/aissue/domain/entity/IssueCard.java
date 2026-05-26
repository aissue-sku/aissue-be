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
@Table(name = "issue_card")
public class IssueCard {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(name = "image_url", length = 2000)
  private String imageUrl;

  @Column(length = 300)
  private String tags; // JSON: ["tag1","tag2","tag3"]

  @Column(length = 50)
  private String category;

  @Column(name = "content_id")
  private Long contentId;

  @Column(name = "source_url", length = 2000)
  private String sourceUrl;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  @Column(name = "snapshot_at", nullable = false)
  private LocalDateTime snapshotAt;

  @Column(name = "rank_order")
  private Integer rankOrder;

  @Column(length = 150)
  private String teaser;
}
