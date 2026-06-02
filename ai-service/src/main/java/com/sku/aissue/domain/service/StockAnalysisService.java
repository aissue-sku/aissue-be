/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sku.aissue.domain.dto.response.StockAnalysisResponse;
import com.sku.aissue.domain.entity.Stock;
import com.sku.aissue.domain.entity.StockPrice;
import com.sku.aissue.domain.repository.StockPriceRepository;
import com.sku.aissue.domain.repository.StockRepository;
import com.sku.aissue.global.client.ContentServiceClient;
import com.sku.aissue.global.client.ContentServiceClient.ContentInfo;
import com.sku.aissue.global.client.KisApiClient;
import com.sku.aissue.global.client.OpenAiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAnalysisService {

  private final StockRepository stockRepository;
  private final StockPriceRepository stockPriceRepository;
  private final KisApiClient kisApiClient;
  private final TechnicalAnalysisService taService;
  private final ContentServiceClient contentServiceClient;
  private final OpenAiClient openAiClient;
  private final ObjectMapper objectMapper;

  private static final String SYSTEM_PROMPT =
      """
      당신은 한국 주식시장 전문가입니다. 뉴스 기사에서 KOSPI 또는 KOSDAQ에 상장된 기업명을 추출합니다.
      반드시 JSON 형식으로만 응답하세요: {"companies": ["기업명1", "기업명2"]}
      규칙:
      - 상장 기업명만 포함 (비상장, 외국 기업 제외)
      - 그룹명이 아닌 상장 법인명 사용 (예: "삼성그룹" → "삼성전자", "삼성SDI" 등으로 분리)
      - 기사와 직접 관련된 기업만 포함 (간접 언급 제외)
      - 최대 3개만 추출
      - 관련 상장 기업이 없으면 {"companies": []} 반환
      """;

  /** contentId로 기사 원문을 가져와 AI로 종목을 추출한 뒤 기술적 분석을 반환합니다. */
  @Transactional
  public List<StockAnalysisResponse> analyzeByContentId(Long contentId) {
    ContentInfo content = contentServiceClient.getContentById(contentId);
    if (content == null) {
      log.warn("기사 조회 실패 - contentId: {}", contentId);
      return List.of();
    }
    return analyzeByText(content.title(), content.body());
  }

  /** 이미 확보한 title+body로 직접 종목 추출 및 기술적 분석을 반환합니다. (submit 흐름에서 사용) */
  @Transactional
  public List<StockAnalysisResponse> analyzeByText(String title, String body) {
    List<String> companyNames = extractCompaniesWithAI(title, body);
    if (companyNames.isEmpty()) {
      log.info("AI 종목 추출 결과 없음 - title: {}", title);
      return List.of();
    }

    log.info("AI 추출 종목: {} - title: {}", companyNames, title);
    return findAndAnalyze(companyNames);
  }

  @Transactional
  public StockAnalysisResponse analyzeByCode(String code) {
    Stock stock =
        stockRepository
            .findById(code)
            .orElseThrow(() -> new IllegalArgumentException("종목 없음: " + code));
    return analyzeStock(stock);
  }

  private List<String> extractCompaniesWithAI(String title, String body) {
    // 원문이 길면 앞부분만 사용 (토큰 절약)
    String articleText = buildArticleText(title, body);
    try {
      String json =
          openAiClient.chat(
              SYSTEM_PROMPT, "다음 기사에서 KOSPI/KOSDAQ 상장 기업명을 추출해주세요.\n\n" + articleText);
      Map<String, List<String>> result = objectMapper.readValue(json, new TypeReference<>() {});
      List<String> companies = result.getOrDefault("companies", List.of());
      return companies.stream().filter(s -> s != null && !s.isBlank()).toList();
    } catch (Exception e) {
      log.warn("AI 종목 추출 실패, 제목 키워드 매칭으로 폴백: {}", e.getMessage());
      // OpenAI 실패 시 제목 키워드 매칭으로 폴백
      return stockRepository.findByNameContainedIn(title).stream()
          .limit(3)
          .map(Stock::getName)
          .toList();
    }
  }

  private String buildArticleText(String title, String body) {
    StringBuilder sb = new StringBuilder("제목: ").append(title);
    if (body != null && !body.isBlank()) {
      String trimmedBody = body.length() > 2000 ? body.substring(0, 2000) : body;
      sb.append("\n\n본문:\n").append(trimmedBody);
    }
    return sb.toString();
  }

  private List<StockAnalysisResponse> findAndAnalyze(List<String> companyNames) {
    Set<Stock> stocks = new LinkedHashSet<>();
    for (String name : companyNames) {
      // 정확한 이름 매칭 우선, 그 다음 포함 매칭
      List<Stock> matched = stockRepository.findByNameContainedIn(name);
      if (!matched.isEmpty()) stocks.add(matched.get(0));
    }

    List<StockAnalysisResponse> result = new ArrayList<>();
    for (Stock stock : stocks) {
      try {
        result.add(analyzeStock(stock));
      } catch (Exception e) {
        log.error("종목 분석 실패 - code: {}, error: {}", stock.getCode(), e.getMessage());
      }
    }
    return result;
  }

  private StockAnalysisResponse analyzeStock(Stock stock) {
    List<StockPrice> prices = getPrices(stock);
    return taService.analyze(stock, prices);
  }

  private List<StockPrice> getPrices(Stock stock) {
    boolean needsRefresh =
        stock.getLastPriceUpdated() == null
            || stock.getLastPriceUpdated().isBefore(LocalDateTime.now().minusHours(6));

    if (needsRefresh) {
      log.info("KIS API 가격 조회 - code: {}", stock.getCode());
      List<StockPrice> fetched = kisApiClient.fetchDailyPrices(stock.getCode());
      if (!fetched.isEmpty()) {
        stockPriceRepository.deleteByStockCode(stock.getCode());
        stockPriceRepository.saveAll(fetched);
        stock.markPriceUpdated();
        stockRepository.save(stock);
        return fetched;
      }
    }

    List<StockPrice> cached =
        stockPriceRepository.findByStockCodeOrderByTradeDateDesc(stock.getCode());
    if (!cached.isEmpty()) return cached;

    return kisApiClient.fetchDailyPrices(stock.getCode());
  }
}
