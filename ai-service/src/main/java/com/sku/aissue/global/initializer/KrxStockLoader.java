/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.initializer;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.sku.aissue.domain.entity.Stock;
import com.sku.aissue.domain.repository.StockRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KrxStockLoader implements ApplicationRunner {

  private final StockRepository stockRepository;
  private final WebClient.Builder webClientBuilder;

  @Override
  public void run(ApplicationArguments args) {
    if (stockRepository.count() > 0) {
      log.info("종목 DB 이미 초기화됨 ({}개), KRX 로딩 건너뜀", stockRepository.count());
      return;
    }
    log.info("KIND 종목 리스트 초기화 시작...");
    List<Stock> stocks = new ArrayList<>();
    stocks.addAll(fetchFromKind("stockMkt", "KOSPI"));
    stocks.addAll(fetchFromKind("kosdaqMkt", "KOSDAQ"));

    if (!stocks.isEmpty()) {
      stockRepository.saveAll(stocks);
      log.info("KIND 종목 초기화 완료 - {}개 저장", stocks.size());
    } else {
      log.warn("KIND API 응답 없음, 종목 초기화 실패");
    }
  }

  private List<Stock> fetchFromKind(String marketType, String market) {
    WebClient kindClient =
        webClientBuilder
            .baseUrl("https://kind.krx.co.kr")
            .defaultHeader("User-Agent", "Mozilla/5.0")
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)) // 5MB
                    .build())
            .build();

    try {
      byte[] responseBytes =
          kindClient
              .get()
              .uri(
                  "/corpgeneral/corpList.do?method=download&searchType=13&marketType=" + marketType)
              .retrieve()
              .bodyToMono(byte[].class)
              .block();

      if (responseBytes == null) return List.of();

      // KIND API 응답은 EUC-KR 인코딩
      String html = new String(responseBytes, Charset.forName("EUC-KR"));
      Document doc = Jsoup.parse(html);
      Elements rows = doc.select("table tr");

      List<Stock> stocks = new ArrayList<>();
      for (Element row : rows) {
        Elements tds = row.select("td");
        if (tds.size() < 3) continue;
        String name = tds.get(0).text().trim();
        String code = tds.get(2).text().trim();
        if (name.isEmpty() || code.isEmpty() || !code.matches("\\d{6}")) continue;
        stocks.add(Stock.builder().code(code).name(name).market(market).build());
      }

      log.info("KIND {} 종목 조회 완료 - {}개", market, stocks.size());
      return stocks;

    } catch (Exception e) {
      log.error("KIND {} 종목 조회 실패: {}", market, e.getMessage());
      return List.of();
    }
  }
}
