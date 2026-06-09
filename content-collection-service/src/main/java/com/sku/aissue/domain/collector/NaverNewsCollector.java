/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.collector;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sku.aissue.domain.dto.CollectedContentDto;
import com.sku.aissue.domain.entity.ContentSource;
import com.sku.aissue.domain.entity.ContentType;
import com.sku.aissue.global.config.NaverApiProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NaverNewsCollector implements ContentCollector {

  private static final String[] TRENDING_KEYWORDS = {
    "이슈", "사건", "속보", "긴급", "주요",
    "헥토", "헥토이노베이션", "헥토파이낸셜", "헥토데이터"
  };
  private static final int DISPLAY_COUNT = 10;
  private static final DateTimeFormatter NAVER_DATE_FORMAT =
      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

  private final RestTemplate restTemplate;
  private final NaverApiProperties naverApiProperties;

  public NaverNewsCollector(
      @Qualifier("externalRestTemplate") RestTemplate restTemplate,
      NaverApiProperties naverApiProperties) {
    this.restTemplate = restTemplate;
    this.naverApiProperties = naverApiProperties;
  }

  @Override
  public List<CollectedContentDto> collect() {
    return Arrays.stream(TRENDING_KEYWORDS)
        .flatMap(keyword -> fetchByKeyword(keyword).stream())
        .distinct()
        .toList();
  }

  @Override
  public ContentSource getSource() {
    return ContentSource.NAVER_NEWS;
  }

  private List<CollectedContentDto> fetchByKeyword(String keyword) {
    try {
      URI uri =
          UriComponentsBuilder.fromUriString(naverApiProperties.getNewsUrl())
              .queryParam("query", keyword)
              .queryParam("display", DISPLAY_COUNT)
              .queryParam("sort", "date")
              .encode(StandardCharsets.UTF_8)
              .build()
              .toUri();

      HttpHeaders headers = new HttpHeaders();
      headers.set("X-Naver-Client-Id", naverApiProperties.getClientId());
      headers.set("X-Naver-Client-Secret", naverApiProperties.getClientSecret());

      ResponseEntity<NaverNewsResponse> response =
          restTemplate.exchange(
              uri, HttpMethod.GET, new HttpEntity<>(headers), NaverNewsResponse.class);

      NaverNewsResponse body = response.getBody();
      if (body == null || body.getItems() == null) {
        return Collections.emptyList();
      }

      return body.getItems().stream().map(this::toDto).toList();

    } catch (Exception e) {
      log.info("네이버 뉴스 수집 실패 - keyword: {}, error: {}", keyword, e.getMessage());
      return Collections.emptyList();
    }
  }

  private CollectedContentDto toDto(NaverNewsItem item) {
    return CollectedContentDto.builder()
        .title(stripHtmlTags(item.getTitle()))
        .body(stripHtmlTags(item.getDescription()))
        .url(item.getLink())
        .source(item.getSource())
        .contentType(ContentType.NEWS)
        .contentSource(ContentSource.NAVER_NEWS)
        .publishedAt(parsePubDate(item.getPubDate()))
        .build();
  }

  private String stripHtmlTags(String text) {
    if (text == null) return null;
    return text.replaceAll("<[^>]+>", "").replaceAll("&[a-zA-Z]+;", " ").trim();
  }

  private LocalDateTime parsePubDate(String pubDate) {
    try {
      return ZonedDateTime.parse(pubDate, NAVER_DATE_FORMAT).toLocalDateTime();
    } catch (Exception e) {
      return LocalDateTime.now();
    }
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class NaverNewsResponse {
    private List<NaverNewsItem> items;
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class NaverNewsItem {
    private String title;
    private String link;
    private String description;
    private String pubDate;
    private String originallink;

    public String getSource() {
      if (originallink == null || originallink.isBlank()) return "네이버뉴스";
      try {
        String host = URI.create(originallink).getHost();
        if (host == null) return "네이버뉴스";
        return host.startsWith("www.") ? host.substring(4) : host;
      } catch (Exception e) {
        return "네이버뉴스";
      }
    }
  }
}
