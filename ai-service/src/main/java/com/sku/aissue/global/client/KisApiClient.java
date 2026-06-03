/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.sku.aissue.domain.entity.StockPrice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KisApiClient {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final WebClient webClient;
  private final String appKey;
  private final String appSecret;

  private final AtomicReference<String> cachedToken = new AtomicReference<>();
  private volatile LocalDateTime tokenExpiry = LocalDateTime.MIN;

  public KisApiClient(
      WebClient.Builder builder,
      @Value("${kis.api.base-url}") String baseUrl,
      @Value("${kis.api.app-key}") String appKey,
      @Value("${kis.api.app-secret}") String appSecret) {
    this.webClient = builder.baseUrl(baseUrl).build();
    this.appKey = appKey;
    this.appSecret = appSecret;
  }

  private String getAccessToken() {
    if (cachedToken.get() != null && LocalDateTime.now().isBefore(tokenExpiry)) {
      return cachedToken.get();
    }
    try {
      Map<?, ?> response =
          webClient
              .post()
              .uri("/oauth2/tokenP")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(
                  Map.of(
                      "grant_type", "client_credentials",
                      "appkey", appKey,
                      "appsecret", appSecret))
              .retrieve()
              .bodyToMono(Map.class)
              .timeout(Duration.ofSeconds(5))
              .block();

      String token = (String) response.get("access_token");
      cachedToken.set(token);
      tokenExpiry = LocalDateTime.now().plusHours(23);
      log.info("KIS API access token 발급 완료");
      return token;
    } catch (Exception e) {
      log.error("KIS API token 발급 실패: {}", e.getMessage());
      throw new RuntimeException("KIS API 인증 실패", e);
    }
  }

  /** 일봉 OHLCV 데이터 조회 (최근 150거래일) */
  @SuppressWarnings("unchecked")
  public List<StockPrice> fetchDailyPrices(String stockCode) {
    String token = getAccessToken();
    String endDate = LocalDate.now().format(DATE_FMT);
    String startDate = LocalDate.now().minusDays(210).format(DATE_FMT); // 영업일 기준 150개 확보

    try {
      Map<?, ?> response =
          webClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                          .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                          .queryParam("FID_INPUT_ISCD", stockCode)
                          .queryParam("FID_INPUT_DATE_1", startDate)
                          .queryParam("FID_INPUT_DATE_2", endDate)
                          .queryParam("FID_PERIOD_DIV_CODE", "D")
                          .queryParam("FID_ORG_ADJ_PRC", "0")
                          .build())
              .header("authorization", "Bearer " + token)
              .header("appkey", appKey)
              .header("appsecret", appSecret)
              .header("tr_id", "FHKST03010100")
              .retrieve()
              .bodyToMono(Map.class)
              .timeout(Duration.ofSeconds(5))
              .block();

      if (response == null || !"0".equals(response.get("rt_cd"))) {
        log.warn("KIS API 응답 오류 - code: {}, msg: {}", stockCode, response);
        return List.of();
      }

      List<Map<String, String>> output2 = (List<Map<String, String>>) response.get("output2");
      if (output2 == null || output2.isEmpty()) return List.of();

      return output2.stream()
          .filter(row -> isValidRow(row))
          .map(row -> toStockPrice(stockCode, row))
          .toList();

    } catch (Exception e) {
      log.error("KIS API 일봉 조회 실패 - code: {}, error: {}", stockCode, e.getMessage());
      return List.of();
    }
  }

  private boolean isValidRow(Map<String, String> row) {
    String close = row.get("stck_clpr");
    return close != null && !close.isBlank() && !"0".equals(close);
  }

  private StockPrice toStockPrice(String stockCode, Map<String, String> row) {
    return StockPrice.builder()
        .stockCode(stockCode)
        .tradeDate(LocalDate.parse(row.get("stck_bsop_date"), DATE_FMT))
        .open(new java.math.BigDecimal(row.getOrDefault("stck_oprc", "0")))
        .high(new java.math.BigDecimal(row.getOrDefault("stck_hgpr", "0")))
        .low(new java.math.BigDecimal(row.getOrDefault("stck_lwpr", "0")))
        .close(new java.math.BigDecimal(row.getOrDefault("stck_clpr", "0")))
        .volume(Long.parseLong(row.getOrDefault("acml_vol", "0")))
        .build();
  }
}
