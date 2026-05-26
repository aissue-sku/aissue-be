/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "알림 정보")
public class NotificationResponse {

  @Schema(description = "알림 ID", example = "1")
  private Long id;

  @Schema(description = "매칭된 구독 키워드", example = "삼성전자")
  private String keyword;

  @Schema(description = "이슈 카드 제목", example = "삼성전자 파업 터졌다!")
  private String title;

  @Schema(description = "원문 기사 URL")
  private String url;

  @Schema(description = "콘텐츠 ID (상세 분석 조회용)")
  private Long contentId;

  @Schema(description = "읽음 여부", example = "false")
  private boolean isRead;

  @Schema(description = "알림 생성 시각")
  private LocalDateTime createdAt;
}
