/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import java.time.LocalDateTime;

import com.sku.aissue.domain.entity.Notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "알림 정보")
public class NotificationResponse {

  @Schema(description = "알림 ID", example = "1")
  private final Long id;

  @Schema(description = "매칭된 구독 키워드", example = "트럼프")
  private final String keyword;

  @Schema(description = "관련 기사 제목", example = "트럼프, 관세 25% 전격 발표")
  private final String contentTitle;

  @Schema(description = "관련 기사 URL")
  private final String contentUrl;

  @Schema(description = "읽음 여부", example = "false")
  private final boolean read;

  @Schema(description = "알림 생성 일시")
  private final LocalDateTime createdAt;

  public static NotificationResponse from(Notification notification) {
    return NotificationResponse.builder()
        .id(notification.getId())
        .keyword(notification.getKeyword())
        .contentTitle(notification.getContentTitle())
        .contentUrl(notification.getContentUrl())
        .read(notification.isRead())
        .createdAt(notification.getCreatedAt())
        .build();
  }
}
