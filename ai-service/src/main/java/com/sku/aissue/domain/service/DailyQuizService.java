/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sku.aissue.domain.dto.request.QuizAnswerRequest;
import com.sku.aissue.domain.dto.response.QuizAnswerResponse;
import com.sku.aissue.domain.dto.response.QuizResponse;
import com.sku.aissue.domain.dto.response.QuizResponse.QuizArticle;
import com.sku.aissue.domain.entity.DailyQuiz;
import com.sku.aissue.domain.entity.QuizAttempt;
import com.sku.aissue.domain.exception.AnalysisErrorCode;
import com.sku.aissue.domain.repository.DailyQuizRepository;
import com.sku.aissue.domain.repository.QuizAttemptRepository;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.global.client.ContentServiceClient;
import com.sku.aissue.global.client.ContentServiceClient.ContentInfo;
import com.sku.aissue.global.client.OpenAiClient;
import com.sku.aissue.global.client.UserServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyQuizService {

  private static final int QUIZ_POINTS = 50;

  private static final String SYSTEM_PROMPT =
      """
      당신은 가짜뉴스 탐지 교육용 퀴즈를 만드는 전문가입니다.
      실제 뉴스 기사 2개를 읽고 아래 작업을 수행하세요.

      [작업 1] 각 기사를 3-4문장으로 요약하세요.
      - 독자가 핵심 내용을 파악할 수 있어야 합니다.
      - 기사의 문체(보도체)를 그대로 유지하세요.

      [작업 2] 가짜 기사 1개를 생성하세요.
      - 실제 기사와 동일한 보도체·어투로 작성하세요. "~했다", "~밝혔다" 등 뉴스 문체 필수.
      - 제목도 실제 뉴스 헤드라인 형식("[단독]", "…" 등 활용)으로 작성하세요.
      - 실제로 존재할 법한 구체적인 수치, 인명, 지명을 사용하세요.
      - 조작 방식: 수치 변경, 결과 반전, 발언 왜곡, 주체 교체 중 하나를 택하세요.
      - 절대 "가짜", "허위", "조작" 같은 단어를 본문에 쓰지 마세요.
      - 3-4문장으로 작성하세요.

      [작업 3] 가짜 기사 해설을 작성하세요. 사용자 학습이 목적이므로 충분히 구체적으로 작성합니다.
      - 다음 3가지를 반드시 포함하세요:
        (1) 어떤 부분이 조작되었는지 — 구체적 문장이나 수치를 인용
        (2) 사실은 무엇인지 — 진실에 가까운 정보로 대비 제시
        (3) 이런 조작을 알아채는 팁 — 일반화 가능한 가짜뉴스 판별 포인트
      - 분량: 3~5문장, 200~350자
      - 친근한 설명체("~예요", "~죠" 등 사용 가능)로 작성해 학습 부담을 낮추세요.

      반드시 JSON 형식으로만 응답하세요:
      {
        "summary1": "기사1 요약 (3-4문장, 보도체)",
        "summary2": "기사2 요약 (3-4문장, 보도체)",
        "fakeTitle": "가짜 기사 제목 (실제 뉴스 헤드라인 형식)",
        "fakeBody": "가짜 기사 내용 (3-4문장, 보도체)",
        "explanation": "가짜 기사 해설 (3~5문장, 조작 부분 + 실제 사실 + 판별 팁 포함)"
      }
      """;

  private final DailyQuizRepository dailyQuizRepository;
  private final QuizAttemptRepository quizAttemptRepository;
  private final ContentServiceClient contentServiceClient;
  private final OpenAiClient openAiClient;
  private final UserServiceClient userServiceClient;
  private final ObjectMapper objectMapper;

  @Transactional
  public QuizResponse getTodayQuiz(String username) {
    LocalDate today = LocalDate.now();
    DailyQuiz quiz =
        dailyQuizRepository.findByQuizDate(today).orElseGet(() -> generateAndSave(today));

    List<QuizArticle> articles = deserializeArticles(quiz.getArticlesJson());

    QuizAttempt attempt =
        username != null
            ? quizAttemptRepository.findByUsernameAndQuizDate(username, today).orElse(null)
            : null;

    return QuizResponse.builder()
        .quizId(quiz.getId())
        .articles(articles)
        .alreadyAnswered(attempt != null)
        .myAnswer(attempt != null ? attempt.getSelectedIndex() : null)
        .myAnswerCorrect(attempt != null && attempt.isCorrect())
        .fakeIndex(attempt != null ? quiz.getFakeIndex() : null)
        .explanation(attempt != null ? quiz.getFakeExplanation() : null)
        .build();
  }

  @Transactional
  public QuizAnswerResponse submitAnswer(String username, QuizAnswerRequest request) {
    LocalDate today = LocalDate.now();

    if (quizAttemptRepository.existsByUsernameAndQuizDate(username, today)) {
      throw new CustomException(AnalysisErrorCode.QUIZ_ALREADY_ANSWERED);
    }

    DailyQuiz quiz =
        dailyQuizRepository
            .findByQuizDate(today)
            .orElseThrow(() -> new CustomException(AnalysisErrorCode.QUIZ_NOT_FOUND));

    boolean correct = request.getSelectedIndex() == quiz.getFakeIndex();
    int pointsEarned = correct ? QUIZ_POINTS : 0;

    quizAttemptRepository.save(
        QuizAttempt.builder()
            .username(username)
            .quizDate(today)
            .selectedIndex(request.getSelectedIndex())
            .correct(correct)
            .answeredAt(LocalDateTime.now())
            .build());

    if (correct) {
      userServiceClient.addPoints(username, pointsEarned, "일일 퀴즈 정답");
    }

    return QuizAnswerResponse.builder()
        .correct(correct)
        .fakeIndex(quiz.getFakeIndex())
        .pointsEarned(pointsEarned)
        .explanation(quiz.getFakeExplanation())
        .build();
  }

  @Transactional
  public QuizResponse generateToday() {
    LocalDate today = LocalDate.now();
    quizAttemptRepository.deleteByQuizDate(today);
    dailyQuizRepository.findByQuizDate(today).ifPresent(dailyQuizRepository::delete);
    dailyQuizRepository.flush();
    DailyQuiz quiz = generateAndSave(today);
    log.info("오늘의 퀴즈 수동 재생성 완료 - 기존 응답 모두 초기화");
    return QuizResponse.builder()
        .quizId(quiz.getId())
        .articles(deserializeArticles(quiz.getArticlesJson()))
        .alreadyAnswered(false)
        .build();
  }

  private DailyQuiz generateAndSave(LocalDate date) {
    log.info("오늘의 퀴즈 생성 시작 - date: {}", date);

    List<ContentInfo> recentArticles = contentServiceClient.findPaged(0, 10).contents();
    if (recentArticles.size() < 2) {
      throw new CustomException(AnalysisErrorCode.QUIZ_GENERATION_FAILED);
    }

    ContentInfo article1 = recentArticles.get(0);
    ContentInfo article2 = recentArticles.get(1);

    Map<String, String> generated = generateQuizContent(article1, article2);

    // 진짜 2개 + 가짜 1개를 셔플 (ThreadLocalRandom 명시 — 기본 RNG 편향 회피)
    record ArticleData(String title, String body, boolean fake) {}
    List<ArticleData> items = new ArrayList<>();
    items.add(new ArticleData(article1.title(), generated.get("summary1"), false));
    items.add(new ArticleData(article2.title(), generated.get("summary2"), false));
    items.add(new ArticleData(generated.get("fakeTitle"), generated.get("fakeBody"), true));
    Collections.shuffle(items, ThreadLocalRandom.current());

    int fakeIndex = 0;
    List<QuizArticle> articles = new ArrayList<>();
    for (int i = 0; i < items.size(); i++) {
      ArticleData item = items.get(i);
      if (item.fake()) fakeIndex = i;
      articles.add(QuizArticle.builder().index(i).title(item.title()).body(item.body()).build());
    }

    String articlesJson;
    try {
      articlesJson = objectMapper.writeValueAsString(articles);
    } catch (Exception e) {
      throw new CustomException(AnalysisErrorCode.QUIZ_GENERATION_FAILED);
    }

    DailyQuiz quiz =
        DailyQuiz.builder()
            .quizDate(date)
            .articlesJson(articlesJson)
            .fakeIndex(fakeIndex)
            .fakeExplanation(generated.get("explanation"))
            .build();

    log.info("오늘의 퀴즈 생성 완료 - fakeIndex: {}", fakeIndex);
    return dailyQuizRepository.save(quiz);
  }

  private Map<String, String> generateQuizContent(ContentInfo a1, ContentInfo a2) {
    String prompt =
        String.format(
            "다음 실제 뉴스 기사 2개를 요약하고, 가짜 뉴스를 생성해주세요.\n\n"
                + "기사1 제목: %s\n기사1 본문: %s\n\n"
                + "기사2 제목: %s\n기사2 본문: %s",
            a1.title(), truncate(a1.body(), 500), a2.title(), truncate(a2.body(), 500));

    String json = openAiClient.chat(SYSTEM_PROMPT, prompt);
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      log.error("퀴즈 콘텐츠 파싱 실패: {}", e.getMessage());
      throw new CustomException(AnalysisErrorCode.QUIZ_GENERATION_FAILED);
    }
  }

  private String truncate(String text, int max) {
    if (text == null) return "";
    return text.length() > max ? text.substring(0, max) + "..." : text;
  }

  private List<QuizArticle> deserializeArticles(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      log.error("퀴즈 기사 역직렬화 실패: {}", e.getMessage());
      return List.of();
    }
  }
}
