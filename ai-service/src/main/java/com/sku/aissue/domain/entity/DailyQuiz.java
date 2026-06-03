/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
    name = "daily_quiz",
    uniqueConstraints = {@UniqueConstraint(columnNames = "quiz_date")})
public class DailyQuiz {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "quiz_date", nullable = false)
  private LocalDate quizDate;

  // 실제 기사 2개 + 가짜 기사 1개를 셔플한 순서대로 JSON 배열로 저장
  @Column(name = "articles_json", columnDefinition = "TEXT", nullable = false)
  private String articlesJson;

  // 셔플된 배열에서 가짜 기사의 인덱스 (0, 1, 2)
  @Column(name = "fake_index", nullable = false)
  private int fakeIndex;

  // 가짜 기사 생성 근거 (정답 공개 시 보여줄 설명)
  @Column(name = "fake_explanation", columnDefinition = "TEXT")
  private String fakeExplanation;
}
