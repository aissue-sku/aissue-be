/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto;

import java.time.LocalDateTime;

import com.sku.aissue.domain.entity.ContentSource;
import com.sku.aissue.domain.entity.ContentType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CollectedContentDto {

  private String title;
  private String body;
  private String url;
  private String source;
  private String category;
  private ContentType contentType;
  private ContentSource contentSource;
  private LocalDateTime publishedAt;
}
