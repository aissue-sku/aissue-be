/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "키워드 관련 뉴스 아이템")
public class NewsItemResponse {

  @Schema(description = "콘텐츠 ID", example = "42")
  private final String id;

  @Schema(description = "기사 제목", example = "전기차 화재 예방 가이드라인, 시민들이 꼭 알아야 할 사항")
  private final String title;

  @Schema(description = "썸네일 이미지 URL (없으면 null)")
  private final String imageUrl;

  @Schema(description = "발행 시간 (상대 시간)", example = "5분 전")
  private final String timeAgo;

  @Schema(description = "원본 기사 URL", example = "https://news.naver.com/article/001/0001234567")
  private final String url;
}
