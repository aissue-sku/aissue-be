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
    name = "notifications",
    indexes = {@Index(columnList = "user_id"), @Index(columnList = "created_at")})
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, length = 20)
  private String userId;

  @Column(nullable = false, length = 50)
  private String keyword;

  @Column(nullable = false)
  private String title;

  @Column(length = 768)
  private String url;

  @Column(name = "content_id")
  private Long contentId;

  @Column(name = "is_read", nullable = false)
  @Builder.Default
  private boolean isRead = false;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  public void markAsRead() {
    this.isRead = true;
  }
}
