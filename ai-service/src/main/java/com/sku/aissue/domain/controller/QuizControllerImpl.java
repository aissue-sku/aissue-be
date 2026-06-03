/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.request.QuizAnswerRequest;
import com.sku.aissue.domain.dto.response.QuizAnswerResponse;
import com.sku.aissue.domain.dto.response.QuizResponse;
import com.sku.aissue.domain.service.DailyQuizService;
import com.sku.aissue.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class QuizControllerImpl implements QuizController {

  private final DailyQuizService dailyQuizService;

  @Override
  public ResponseEntity<BaseResponse<QuizResponse>> generateTodayQuiz() {
    return ResponseEntity.ok(BaseResponse.success(dailyQuizService.generateToday()));
  }

  @Override
  public ResponseEntity<BaseResponse<QuizResponse>> getTodayQuiz(
      @AuthenticationPrincipal String username) {
    return ResponseEntity.ok(BaseResponse.success(dailyQuizService.getTodayQuiz(username)));
  }

  @Override
  public ResponseEntity<BaseResponse<QuizAnswerResponse>> submitAnswer(
      QuizAnswerRequest request, @AuthenticationPrincipal String username) {
    return ResponseEntity.ok(
        BaseResponse.success(dailyQuizService.submitAnswer(username, request)));
  }
}
