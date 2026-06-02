/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.dto.response;

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
  private Indicators indicators;
  private Signals signals;
  private FibonacciLevels fibonacci;
  private int overallScore;
  private String recommendation; // BUY / WATCH / SELL
  private String summary;

  @Getter
  @Builder
  @Jacksonized
  public static class Indicators {
    private Long ma5;
    private Long ma20;
    private Long ma60;
    private Long ma120;
    private Double rsi;
    private Double macd;
    private Double macdSignal;
    private Double macdHistogram;
    private Long bollingerUpper;
    private Long bollingerMiddle;
    private Long bollingerLower;
    private Double adx;
  }

  @Getter
  @Builder
  @Jacksonized
  public static class Signals {
    private String trend; // BULLISH / BEARISH / NEUTRAL
    private boolean goldenCross;
    private boolean deadCross;
    private String rsiSignal; // OVERBOUGHT / OVERSOLD / NEUTRAL
    private String macdSignal; // BUY / SELL / NEUTRAL
    private String bollingerSignal; // UPPER / MIDDLE / LOWER
    private boolean volumeSpike;
    private String adxStrength; // VERY_STRONG / STRONG / MODERATE / WEAK
  }

  @Getter
  @Builder
  @Jacksonized
  public static class FibonacciLevels {
    private Long support;
    private Long resistance;
  }
}
