/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.request.ContentSubmitRequest;
import com.sku.aissue.domain.dto.response.AnalysisHistoryResponse;
import com.sku.aissue.domain.dto.response.AnalysisScoreResponse;
import com.sku.aissue.domain.dto.response.ArticleCritiqueResponse;
import com.sku.aissue.domain.entity.ContentAnalysis;
import com.sku.aissue.domain.repository.ContentAnalysisRepository;
import com.sku.aissue.domain.service.ArticleCritiqueService;
import com.sku.aissue.domain.service.ArticleEmbeddingService;
import com.sku.aissue.domain.service.ArticleExtractor;
import com.sku.aissue.domain.service.ContentAnalysisService;
import com.sku.aissue.global.client.ContentServiceClient;
import com.sku.aissue.global.client.ContentServiceClient.SaveContentRequest;
import com.sku.aissue.global.client.NotificationServiceClient;
import com.sku.aissue.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AnalysisControllerImpl implements AnalysisController {

  private final ContentAnalysisService contentAnalysisService;
  private final ArticleCritiqueService articleCritiqueService;
  private final ArticleEmbeddingService articleEmbeddingService;
  private final ArticleExtractor articleExtractor;
  private final ContentAnalysisRepository contentAnalysisRepository;
  private final ContentServiceClient contentServiceClient;
  private final NotificationServiceClient notificationServiceClient;

  @Override
  public ResponseEntity<BaseResponse<AnalysisScoreResponse>> submit(
      ContentSubmitRequest request, @AuthenticationPrincipal String username) {
    String title;
    String body;
    String url = null;

    if (request.getType() == ContentSubmitRequest.SubmitType.URL) {
      url = request.getContent();
      ArticleExtractor.ExtractedArticle article = articleExtractor.extract(url);
      title = (article.title() != null && !article.title().isBlank()) ? article.title() : url;
      body = article.body();
    } else {
      String text = request.getContent();
      title = text.length() > 50 ? text.substring(0, 50) + "..." : text;
      body = text;
    }

    ContentServiceClient.ContentInfo saved =
        contentServiceClient.saveOrFind(
            new SaveContentRequest(
                title,
                body,
                url,
                username != null ? username : "anonymous",
                "ISSUE",
                "USER_SUBMITTED"));

    AnalysisScoreResponse result = contentAnalysisService.analyze(title, url, body, username);

    notificationServiceClient.sendDirect(username, title, url, saved != null ? saved.id() : null);

    Long contentId = saved != null ? saved.id() : null;
    return ResponseEntity.ok(BaseResponse.success(result.toBuilder().contentId(contentId).build()));
  }

  @Override
  public ResponseEntity<BaseResponse<List<AnalysisHistoryResponse>>> getAnalysisHistory(
      @AuthenticationPrincipal String username) {
    List<AnalysisHistoryResponse> history =
        contentAnalysisRepository.findByUserIdOrderByAnalyzedAtDesc(username).stream()
            .map(
                (ContentAnalysis analysis) -> {
                  Long contentId =
                      analysis.getUrl() != null
                          ? contentServiceClient.findIdByUrl(analysis.getUrl())
                          : null;
                  return AnalysisHistoryResponse.from(analysis, contentId);
                })
            .toList();
    return ResponseEntity.ok(BaseResponse.success(history));
  }

  @Override
  public ResponseEntity<BaseResponse<ArticleCritiqueResponse>> critique(
      ContentSubmitRequest request, @AuthenticationPrincipal String username) {
    String title;
    String body;
    String articleUrl = null;
    ArticleCritiqueResponse result;

    if (request.getType() == ContentSubmitRequest.SubmitType.URL) {
      articleUrl = request.getContent();
      ArticleExtractor.ExtractedArticle article = articleExtractor.extract(articleUrl);
      title =
          (article.title() != null && !article.title().isBlank()) ? article.title() : articleUrl;
      body = article.body();
      result = articleCritiqueService.analyzeWithCache(title, body, articleUrl);
    } else {
      String text = request.getContent();
      title = text.length() > 50 ? text.substring(0, 50) + "..." : text;
      body = text;
      result = articleCritiqueService.analyze(title, body);
    }

    notificationServiceClient.sendDirect(username, title, articleUrl, null);
    return ResponseEntity.ok(BaseResponse.success(result));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> embedAllArticles() {
    articleEmbeddingService.embedAllExisting();
    return ResponseEntity.ok(BaseResponse.success(200, "전체 기사 임베딩이 백그라운드에서 시작되었습니다.", null));
  }

  @Override
  public ResponseEntity<BaseResponse<ArticleCritiqueResponse>> getCritiqueByContentId(Long id) {
    return ResponseEntity.ok(BaseResponse.success(articleCritiqueService.getByContentId(id)));
  }

  @Override
  public ResponseEntity<BaseResponse<AnalysisScoreResponse>> getAnalysisByContentId(Long id) {
    return ResponseEntity.ok(BaseResponse.success(contentAnalysisService.getByContentId(id)));
  }
}
