/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "이슈 카드 응답")
public class IssueCardResponse {

  @Schema(description = "콘텐츠 ID", example = "42")
  private String id;

  @Schema(description = "독자의 궁금증을 자극하는 제목", example = "아이 키우고 집안일 해도 582조의 숨겨진 진실!")
  private String title;

  @Schema(description = "기사를 클릭하게 만드는 궁금증 유발 질문", example = "진짜 피해자는 따로 있다고?")
  private String teaser;

  @Schema(description = "DALL-E로 생성된 주제 관련 이미지 URL")
  private String imageUrl;

  @Schema(description = "게시 후 경과 시간", example = "10분 전")
  private String timeAgo;

  @Schema(description = "연관 키워드 태그 목록 (3개)", example = "[\"육아\", \"가사노동\", \"경제적 가치\"]")
  private List<String> tags;

  @Schema(description = "기사 카테고리", example = "경제")
  private String category;

  @Schema(description = "원문 기사 URL", example = "https://www.hani.co.kr/arti/economy/1234567.html")
  private String url;
}
