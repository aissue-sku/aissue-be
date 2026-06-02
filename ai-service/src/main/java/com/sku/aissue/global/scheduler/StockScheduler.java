/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sku.aissue.domain.entity.Stock;
import com.sku.aissue.domain.entity.StockPrice;
import com.sku.aissue.domain.repository.StockPriceRepository;
import com.sku.aissue.domain.repository.StockRepository;
import com.sku.aissue.global.client.KisApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockScheduler {

  private final StockRepository stockRepository;
  private final StockPriceRepository stockPriceRepository;
  private final KisApiClient kisApiClient;

  // 평일 장 마감 후 16:30 (KST) 가격 갱신
  @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Seoul")
  @Transactional
  public void refreshStockPrices() {
    // 최근 7일 내 조회된 종목만 갱신
    LocalDateTime threshold = LocalDateTime.now().minusDays(7);
    List<Stock> activeStocks =
        stockRepository.findAll().stream()
            .filter(
                s -> s.getLastPriceUpdated() != null && s.getLastPriceUpdated().isAfter(threshold))
            .toList();

    log.info("장 마감 가격 갱신 시작 - {}개 종목", activeStocks.size());

    for (Stock stock : activeStocks) {
      try {
        List<StockPrice> prices = kisApiClient.fetchDailyPrices(stock.getCode());
        if (!prices.isEmpty()) {
          stockPriceRepository.deleteByStockCode(stock.getCode());
          stockPriceRepository.saveAll(prices);
          stock.markPriceUpdated();
          stockRepository.save(stock);
        }
        Thread.sleep(200); // KIS API rate limit 보호
      } catch (Exception e) {
        log.error("가격 갱신 실패 - code: {}, error: {}", stock.getCode(), e.getMessage());
      }
    }

    log.info("장 마감 가격 갱신 완료");
  }
}
