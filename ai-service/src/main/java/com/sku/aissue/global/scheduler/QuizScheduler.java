/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sku.aissue.domain.service.DailyQuizService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizScheduler {

  private final DailyQuizService dailyQuizService;

  // 매일 자정에 다음 날 퀴즈 미리 생성
  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
  public void generateDailyQuiz() {
    log.info("일일 퀴즈 사전 생성 시작");
    try {
      dailyQuizService.getTodayQuiz(null);
      log.info("일일 퀴즈 사전 생성 완료");
    } catch (Exception e) {
      log.error("일일 퀴즈 사전 생성 실패: {}", e.getMessage());
    }
  }
}
