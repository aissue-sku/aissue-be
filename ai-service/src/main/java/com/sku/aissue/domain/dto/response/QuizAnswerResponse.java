/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizAnswerResponse {

  private boolean correct;
  private int fakeIndex; // 정답 (가짜 기사 인덱스)
  private int pointsEarned; // 획득 포인트 (오답이면 0)
  private String explanation; // 왜 가짜인지 설명
}
