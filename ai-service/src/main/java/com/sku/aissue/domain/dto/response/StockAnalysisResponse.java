/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class StockAnalysisResponse {

  private String code;
  private String name;
  private String market;
  private Long currentPrice;
  private Long priceChange;
  private Double priceChangeRate;
  private int overallScore;
  private String recommendation; // BUY / WATCH / SELL
  private String summary;
  private List<KeyPoint> keyPoints;

  @Getter
  @Builder
  @Jacksonized
  public static class KeyPoint {
    private String type; // positive / warning / negative / neutral
    private String text;
  }

  // 내부 계산용 (직렬화 제외)
  @Getter
  @Builder
  public static class Signals {
    private String trend;
    private boolean goldenCross;
    private boolean deadCross;
    private String rsiSignal;
    private String macdSignal;
    private String bollingerSignal;
    private boolean volumeSpike;
    private String adxStrength;
  }

  // 내부 계산용 (직렬화 제외)
  @Getter
  @Builder
  public static class FibonacciLevels {
    private Long support;
    private Long resistance;
  }
}
