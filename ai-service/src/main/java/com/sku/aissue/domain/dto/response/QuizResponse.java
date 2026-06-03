/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizResponse {

  private Long quizId;
  private List<QuizArticle> articles; // 3개 (셔플된 순서)
  private boolean alreadyAnswered;
  private Integer myAnswer; // 이미 답변한 경우 내가 선택한 인덱스
  private boolean myAnswerCorrect; // 이미 답변한 경우 정답 여부

  @Getter
  @Builder
  public static class QuizArticle {
    private int index; // 0, 1, 2
    private String title;
    private String body; // 3-4문장 요약
  }
}
