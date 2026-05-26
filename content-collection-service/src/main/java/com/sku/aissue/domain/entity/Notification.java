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
@Table(name = "notification")
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, length = 100)
  private String userId;

  @Column(nullable = false, length = 50)
  private String keyword;

  @Column(name = "content_title", nullable = false, length = 500)
  private String contentTitle;

  @Column(name = "content_url", length = 2000)
  private String contentUrl;

  @Column(name = "is_read", nullable = false)
  private boolean isRead;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public void markAsRead() {
    this.isRead = true;
  }
}
