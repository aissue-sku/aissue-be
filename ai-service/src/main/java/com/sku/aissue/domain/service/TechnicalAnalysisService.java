/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;

import com.sku.aissue.domain.dto.response.StockAnalysisResponse;
import com.sku.aissue.domain.dto.response.StockAnalysisResponse.FibonacciLevels;
import com.sku.aissue.domain.dto.response.StockAnalysisResponse.KeyPoint;
import com.sku.aissue.domain.dto.response.StockAnalysisResponse.Signals;
import com.sku.aissue.domain.entity.Stock;
import com.sku.aissue.domain.entity.StockPrice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TechnicalAnalysisService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  public StockAnalysisResponse analyze(Stock stock, List<StockPrice> rawPrices) {
    // 날짜 오름차순 정렬 (TA4J는 시간순 필요)
    List<StockPrice> prices =
        rawPrices.stream().sorted(Comparator.comparing(StockPrice::getTradeDate)).toList();

    if (prices.size() < 20) {
      log.warn("데이터 부족 - code: {}, size: {}", stock.getCode(), prices.size());
      return buildInsufficientDataResponse(stock, prices);
    }

    BarSeries series = buildBarSeries(stock.getCode(), prices);
    int last = series.getEndIndex();

    ClosePriceIndicator close = new ClosePriceIndicator(series);

    // 이동평균선
    SMAIndicator ma5 = new SMAIndicator(close, 5);
    SMAIndicator ma20 = new SMAIndicator(close, 20);
    SMAIndicator ma60 = prices.size() >= 60 ? new SMAIndicator(close, 60) : null;
    SMAIndicator ma120 = prices.size() >= 120 ? new SMAIndicator(close, 120) : null;

    double ma5Val = toDouble(ma5.getValue(last));
    double ma20Val = toDouble(ma20.getValue(last));
    double ma60Val = ma60 != null ? toDouble(ma60.getValue(last)) : 0;
    double ma120Val = ma120 != null ? toDouble(ma120.getValue(last)) : 0;

    // RSI(14)
    RSIIndicator rsi = new RSIIndicator(close, 14);
    double rsiVal = toDouble(rsi.getValue(last));

    // MACD(12, 26, 9)
    MACDIndicator macd = new MACDIndicator(close, 12, 26);
    EMAIndicator macdSignal = new EMAIndicator(macd, 9);
    double macdVal = toDouble(macd.getValue(last));
    double macdSignalVal = toDouble(macdSignal.getValue(last));
    double macdHist = macdVal - macdSignalVal;

    // 볼린저밴드(20, 2σ)
    BollingerBandsMiddleIndicator bbMiddle = new BollingerBandsMiddleIndicator(ma20);
    StandardDeviationIndicator stdDev = new StandardDeviationIndicator(close, 20);
    BollingerBandsUpperIndicator bbUpper = new BollingerBandsUpperIndicator(bbMiddle, stdDev);
    BollingerBandsLowerIndicator bbLower = new BollingerBandsLowerIndicator(bbMiddle, stdDev);
    double bbUpperVal = toDouble(bbUpper.getValue(last));
    double bbMiddleVal = toDouble(bbMiddle.getValue(last));
    double bbLowerVal = toDouble(bbLower.getValue(last));

    // ADX(14) - 추세 강도
    ADXIndicator adx = new ADXIndicator(series, 14);
    double adxVal = toDouble(adx.getValue(last));

    // 현재가
    double currentPrice = toDouble(close.getValue(last));
    double prevClose = last > 0 ? toDouble(close.getValue(last - 1)) : currentPrice;
    double priceChange = currentPrice - prevClose;
    double priceChangeRate = prevClose != 0 ? (priceChange / prevClose) * 100 : 0;

    // 거래량 급등 (5일 평균 대비 2배 이상)
    boolean volumeSpike = isVolumeSpike(prices);

    // 골든/데드크로스 (MA5 vs MA20)
    boolean goldenCross = false;
    boolean deadCross = false;
    if (last > 0) {
      double prevMa5 = toDouble(ma5.getValue(last - 1));
      double prevMa20 = toDouble(ma20.getValue(last - 1));
      goldenCross = prevMa5 <= prevMa20 && ma5Val > ma20Val;
      deadCross = prevMa5 >= prevMa20 && ma5Val < ma20Val;
    }

    // 피보나치 지지/저항 (최근 60일 고저 기준)
    FibonacciLevels fibonacci = calcFibonacci(prices, currentPrice);

    // 신호 집계
    Signals signals =
        buildSignals(
            ma5Val,
            ma20Val,
            rsiVal,
            macdVal,
            macdSignalVal,
            macdHist,
            currentPrice,
            bbUpperVal,
            bbLowerVal,
            adxVal,
            goldenCross,
            deadCross,
            volumeSpike);

    // 종합 점수 및 추천
    int score = calcScore(signals, rsiVal, adxVal);
    String recommendation = score >= 65 ? "BUY" : score >= 45 ? "WATCH" : "SELL";
    String summary = buildSummary(signals, rsiVal, adxVal, ma5Val, ma20Val, fibonacci);
    List<KeyPoint> keyPoints =
        buildKeyPoints(signals, rsiVal, adxVal, ma5Val, ma20Val, fibonacci, volumeSpike);

    return StockAnalysisResponse.builder()
        .code(stock.getCode())
        .name(stock.getName())
        .market(stock.getMarket())
        .currentPrice((long) currentPrice)
        .priceChange((long) priceChange)
        .priceChangeRate(Math.round(priceChangeRate * 100.0) / 100.0)
        .overallScore(score)
        .recommendation(recommendation)
        .summary(summary)
        .keyPoints(keyPoints)
        .build();
  }

  private BarSeries buildBarSeries(String name, List<StockPrice> prices) {
    BarSeries series = new BaseBarSeries(name);
    for (StockPrice p : prices) {
      ZonedDateTime dt = p.getTradeDate().atStartOfDay(KST);
      series.addBar(
          dt,
          p.getOpen().doubleValue(),
          p.getHigh().doubleValue(),
          p.getLow().doubleValue(),
          p.getClose().doubleValue(),
          p.getVolume().doubleValue());
    }
    return series;
  }

  private boolean isVolumeSpike(List<StockPrice> prices) {
    if (prices.size() < 6) return false;
    int last = prices.size() - 1;
    double todayVol = prices.get(last).getVolume();
    double avg5 =
        prices.subList(last - 5, last).stream()
            .mapToLong(StockPrice::getVolume)
            .average()
            .orElse(1);
    return todayVol >= avg5 * 2.0;
  }

  private FibonacciLevels calcFibonacci(List<StockPrice> prices, double currentPrice) {
    int window = Math.min(60, prices.size());
    List<StockPrice> recent = prices.subList(prices.size() - window, prices.size());

    double high =
        recent.stream().mapToDouble(p -> p.getHigh().doubleValue()).max().orElse(currentPrice);
    double low =
        recent.stream().mapToDouble(p -> p.getLow().doubleValue()).min().orElse(currentPrice);
    double range = high - low;

    double fib236 = high - 0.236 * range;
    double fib382 = high - 0.382 * range;
    double fib500 = high - 0.500 * range;
    double fib618 = high - 0.618 * range;
    double fib786 = high - 0.786 * range;

    // 현재가 아래에서 가장 가까운 레벨 = 지지선
    double support = closestBelow(currentPrice, fib236, fib382, fib500, fib618, fib786, low);
    // 현재가 위에서 가장 가까운 레벨 = 저항선
    double resistance = closestAbove(currentPrice, fib236, fib382, fib500, fib618, fib786, high);

    return FibonacciLevels.builder().support((long) support).resistance((long) resistance).build();
  }

  private double closestBelow(double price, double... levels) {
    double result = Double.MIN_VALUE;
    for (double lv : levels) {
      if (lv < price && lv > result) result = lv;
    }
    return result == Double.MIN_VALUE ? price * 0.95 : result;
  }

  private double closestAbove(double price, double... levels) {
    double result = Double.MAX_VALUE;
    for (double lv : levels) {
      if (lv > price && lv < result) result = lv;
    }
    return result == Double.MAX_VALUE ? price * 1.05 : result;
  }

  private Signals buildSignals(
      double ma5,
      double ma20,
      double rsi,
      double macd,
      double macdSig,
      double macdHist,
      double price,
      double bbUpper,
      double bbLower,
      double adx,
      boolean goldenCross,
      boolean deadCross,
      boolean volumeSpike) {

    String trend = ma5 > ma20 ? "BULLISH" : ma5 < ma20 ? "BEARISH" : "NEUTRAL";
    String rsiSignal = rsi >= 70 ? "OVERBOUGHT" : rsi <= 30 ? "OVERSOLD" : "NEUTRAL";
    String macdSignal =
        macdHist > 0 && macd > macdSig
            ? "BUY"
            : macdHist < 0 && macd < macdSig ? "SELL" : "NEUTRAL";
    String bollingerSignal = price >= bbUpper ? "UPPER" : price <= bbLower ? "LOWER" : "MIDDLE";
    String adxStrength =
        adx >= 40 ? "VERY_STRONG" : adx >= 25 ? "STRONG" : adx >= 15 ? "MODERATE" : "WEAK";

    return Signals.builder()
        .trend(trend)
        .goldenCross(goldenCross)
        .deadCross(deadCross)
        .rsiSignal(rsiSignal)
        .macdSignal(macdSignal)
        .bollingerSignal(bollingerSignal)
        .volumeSpike(volumeSpike)
        .adxStrength(adxStrength)
        .build();
  }

  private int calcScore(Signals s, double rsi, double adx) {
    int score = 50;
    if (s.isGoldenCross()) score += 20;
    if (s.isDeadCross()) score -= 20;
    if ("BUY".equals(s.getMacdSignal())) score += 15;
    if ("SELL".equals(s.getMacdSignal())) score -= 15;
    if ("BULLISH".equals(s.getTrend())) score += 10;
    if ("BEARISH".equals(s.getTrend())) score -= 10;
    if ("OVERSOLD".equals(s.getRsiSignal())) score += 10; // 과매도 = 반등 기회
    if ("OVERBOUGHT".equals(s.getRsiSignal())) score -= 5;
    if ("LOWER".equals(s.getBollingerSignal())) score += 5; // 하단 밴드 = 반등 가능성
    if ("UPPER".equals(s.getBollingerSignal())) score -= 5;
    if (s.isVolumeSpike() && "BULLISH".equals(s.getTrend())) score += 10;
    if ("STRONG".equals(s.getAdxStrength()) || "VERY_STRONG".equals(s.getAdxStrength())) score += 5;
    return Math.max(0, Math.min(100, score));
  }

  private String buildSummary(
      Signals s, double rsi, double adx, double ma5, double ma20, FibonacciLevels fib) {
    StringBuilder sb = new StringBuilder();
    if (s.isGoldenCross()) sb.append("MA5가 MA20 상향 돌파(골든크로스). ");
    if (s.isDeadCross()) sb.append("MA5가 MA20 하향 돌파(데드크로스). ");
    if ("BULLISH".equals(s.getTrend()))
      sb.append(String.format("단기 상승 추세(MA5 %.0f > MA20 %.0f). ", ma5, ma20));
    if ("BEARISH".equals(s.getTrend()))
      sb.append(String.format("단기 하락 추세(MA5 %.0f < MA20 %.0f). ", ma5, ma20));
    sb.append(String.format("RSI %.1f", rsi));
    if ("OVERBOUGHT".equals(s.getRsiSignal())) sb.append("(과매수 주의). ");
    else if ("OVERSOLD".equals(s.getRsiSignal())) sb.append("(과매도 반등 구간). ");
    else sb.append("(중립 구간). ");
    if ("BUY".equals(s.getMacdSignal())) sb.append("MACD 매수 신호 발생. ");
    if ("SELL".equals(s.getMacdSignal())) sb.append("MACD 매도 신호 발생. ");
    if (s.isVolumeSpike()) sb.append("거래량 급등(5일 평균 2배 이상). ");
    sb.append(String.format("피보나치 지지 %,d원 / 저항 %,d원.", fib.getSupport(), fib.getResistance()));
    return sb.toString().trim();
  }

  private List<KeyPoint> buildKeyPoints(
      Signals s,
      double rsi,
      double adx,
      double ma5,
      double ma20,
      FibonacciLevels fib,
      boolean volumeSpike) {
    List<KeyPoint> points = new ArrayList<>();

    // 추세
    if (s.isGoldenCross()) {
      points.add(kp("positive", "단기 이동평균이 중기를 상향 돌파(골든크로스) — 상승 전환 신호"));
    } else if (s.isDeadCross()) {
      points.add(kp("negative", "단기 이동평균이 중기를 하향 돌파(데드크로스) — 하락 전환 신호"));
    } else if ("BULLISH".equals(s.getTrend())) {
      points.add(
          kp(
              "positive",
              String.format(
                  "단기·중기 이동평균 정배열 (MA5 %,d > MA20 %,d) — 상승 흐름 유지 중", (long) ma5, (long) ma20)));
    } else if ("BEARISH".equals(s.getTrend())) {
      points.add(
          kp(
              "negative",
              String.format(
                  "단기·중기 이동평균 역배열 (MA5 %,d < MA20 %,d) — 하락 압력 존재", (long) ma5, (long) ma20)));
    }

    // MACD
    if ("BUY".equals(s.getMacdSignal())) {
      points.add(kp("positive", "MACD가 시그널선을 상향 돌파 — 단기 매수세 강화 중"));
    } else if ("SELL".equals(s.getMacdSignal())) {
      points.add(kp("negative", "MACD가 시그널선을 하향 돌파 — 단기 매도 압력 증가"));
    }

    // RSI
    if ("OVERBOUGHT".equals(s.getRsiSignal())) {
      points.add(kp("warning", String.format("RSI %.1f — 과매수 구간(70 이상), 단기 조정 가능성 있음", rsi)));
    } else if ("OVERSOLD".equals(s.getRsiSignal())) {
      points.add(kp("positive", String.format("RSI %.1f — 과매도 구간(30 이하), 반등 기회 가능성", rsi)));
    }

    // 볼린저밴드
    if ("UPPER".equals(s.getBollingerSignal())) {
      points.add(kp("warning", "볼린저밴드 상단 돌파 — 강한 상승이나 단기 과열 주의"));
    } else if ("LOWER".equals(s.getBollingerSignal())) {
      points.add(kp("positive", "볼린저밴드 하단 접근 — 반등 가능성, 저점 매수 고려 구간"));
    }

    // 거래량
    if (volumeSpike && "BULLISH".equals(s.getTrend())) {
      points.add(kp("positive", "거래량 급증(5일 평균 2배 이상) + 상승 추세 — 강한 매수 신호"));
    } else if (volumeSpike) {
      points.add(kp("warning", "거래량 급증 — 방향 확인 필요"));
    }

    // ADX (추세 강도)
    if ("VERY_STRONG".equals(s.getAdxStrength()) || "STRONG".equals(s.getAdxStrength())) {
      points.add(kp("neutral", String.format("ADX %.1f — 추세 강도 강함, 현재 방향성이 뚜렷한 구간", adx)));
    } else if ("WEAK".equals(s.getAdxStrength())) {
      points.add(kp("neutral", String.format("ADX %.1f — 추세 약함, 횡보 가능성 높음", adx)));
    }

    // 피보나치 지지/저항
    points.add(
        kp(
            "neutral",
            String.format(
                "주요 지지선 %,d원 / 저항선 %,d원 (피보나치 기준)", fib.getSupport(), fib.getResistance())));

    return points;
  }

  private KeyPoint kp(String type, String text) {
    return KeyPoint.builder().type(type).text(text).build();
  }

  private StockAnalysisResponse buildInsufficientDataResponse(
      Stock stock, List<StockPrice> prices) {
    long currentPrice = prices.isEmpty() ? 0 : prices.getLast().getClose().longValue();
    return StockAnalysisResponse.builder()
        .code(stock.getCode())
        .name(stock.getName())
        .market(stock.getMarket())
        .currentPrice(currentPrice)
        .priceChange(0L)
        .priceChangeRate(0.0)
        .overallScore(50)
        .recommendation("WATCH")
        .summary("데이터 수집 중입니다. 잠시 후 다시 확인해주세요.")
        .build();
  }

  private double toDouble(org.ta4j.core.num.Num num) {
    return num.doubleValue();
  }
}
