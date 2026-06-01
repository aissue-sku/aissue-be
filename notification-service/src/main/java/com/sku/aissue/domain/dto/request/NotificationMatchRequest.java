/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificationMatchRequest {

  private List<CardInfo> cards;

  @Getter
  @Setter
  @NoArgsConstructor
  public static class CardInfo {
    private String title;
    private String url;
    private Long contentId;
    private List<String> keywords;
  }
}
