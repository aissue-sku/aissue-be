/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentServiceClient {

  private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

  private final RestTemplate restTemplate;

  @Value("${internal.token}")
  private String internalToken;

  private HttpEntity<Void> internalRequest() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(INTERNAL_TOKEN_HEADER, internalToken);
    return new HttpEntity<>(headers);
  }

  private <T> HttpEntity<T> internalRequest(T body) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(INTERNAL_TOKEN_HEADER, internalToken);
    return new HttpEntity<>(body, headers);
  }

  @CircuitBreaker(name = "content", fallbackMethod = "getHotKeywordsFallback")
  public List<String> getHotKeywords() {
    var response =
        restTemplate.exchange(
            "http://content-collection-service/internal/contents/hot-keywords",
            HttpMethod.GET,
            internalRequest(),
            new ParameterizedTypeReference<List<String>>() {});
    List<String> body = response.getBody();
    return body != null ? body : Collections.emptyList();
  }

  private List<String> getHotKeywordsFallback(Exception e) {
    log.warn("content-service 서킷 오픈 - getHotKeywords 빈 목록 반환: {}", e.getMessage());
    return Collections.emptyList();
  }

  @CircuitBreaker(name = "content", fallbackMethod = "getContentByIdFallback")
  public ContentInfo getContentById(Long id) {
    try {
      var response =
          restTemplate.exchange(
              "http://content-collection-service/internal/contents/" + id,
              HttpMethod.GET,
              internalRequest(),
              ContentInfo.class);
      return response.getBody();
    } catch (HttpClientErrorException.NotFound e) {
      return null;
    }
  }

  private ContentInfo getContentByIdFallback(Long id, Exception e) {
    log.warn("content-service 서킷 오픈 - getContentById 스킵 id: {}: {}", id, e.getMessage());
    return null;
  }

  @CircuitBreaker(name = "content", fallbackMethod = "findByIdsFallback")
  public List<ContentInfo> findByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) return Collections.emptyList();
    String idsParam = ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    String url =
        UriComponentsBuilder.fromHttpUrl(
                "http://content-collection-service/internal/contents/by-ids")
            .queryParam("ids", idsParam)
            .toUriString();
    var response =
        restTemplate.exchange(
            url,
            HttpMethod.GET,
            internalRequest(),
            new ParameterizedTypeReference<List<ContentInfo>>() {});
    List<ContentInfo> body = response.getBody();
    return body != null ? body : Collections.emptyList();
  }

  private List<ContentInfo> findByIdsFallback(List<Long> ids, Exception e) {
    log.warn("content-service 서킷 오픈 - findByIds 빈 목록 반환: {}", e.getMessage());
    return Collections.emptyList();
  }

  @CircuitBreaker(name = "content", fallbackMethod = "findByTitleFallback")
  public List<ContentInfo> findByTitle(String keyword, int page, int size) {
    String url =
        UriComponentsBuilder.fromHttpUrl(
                "http://content-collection-service/internal/contents/by-title")
            .queryParam("keyword", keyword)
            .queryParam("page", page)
            .queryParam("size", size)
            .toUriString();
    var response =
        restTemplate.exchange(
            url,
            HttpMethod.GET,
            internalRequest(),
            new ParameterizedTypeReference<List<ContentInfo>>() {});
    List<ContentInfo> body = response.getBody();
    return body != null ? body : Collections.emptyList();
  }

  private List<ContentInfo> findByTitleFallback(String keyword, int page, int size, Exception e) {
    log.warn("content-service 서킷 오픈 - findByTitle 빈 목록 반환: {}", e.getMessage());
    return Collections.emptyList();
  }

  @CircuitBreaker(name = "content", fallbackMethod = "findPagedFallback")
  public PagedContentInfo findPaged(int page, int size) {
    String url =
        UriComponentsBuilder.fromHttpUrl(
                "http://content-collection-service/internal/contents/paged")
            .queryParam("page", page)
            .queryParam("size", size)
            .toUriString();
    var response =
        restTemplate.exchange(url, HttpMethod.GET, internalRequest(), PagedContentInfo.class);
    PagedContentInfo body = response.getBody();
    return body != null ? body : new PagedContentInfo(Collections.emptyList(), false, 0);
  }

  private PagedContentInfo findPagedFallback(int page, int size, Exception e) {
    log.warn("content-service 서킷 오픈 - findPaged 빈 결과 반환: {}", e.getMessage());
    return new PagedContentInfo(Collections.emptyList(), false, 0);
  }

  @CircuitBreaker(name = "content", fallbackMethod = "saveOrFindFallback")
  public ContentInfo saveOrFind(SaveContentRequest request) {
    var response =
        restTemplate.exchange(
            "http://content-collection-service/internal/contents/save-or-find",
            HttpMethod.POST,
            internalRequest(request),
            ContentInfo.class);
    return response.getBody();
  }

  private ContentInfo saveOrFindFallback(SaveContentRequest request, Exception e) {
    log.warn("content-service 서킷 오픈 - saveOrFind 스킵: {}", e.getMessage());
    return null;
  }

  @CircuitBreaker(name = "content", fallbackMethod = "findIdByUrlFallback")
  public Long findIdByUrl(String url) {
    String requestUrl =
        UriComponentsBuilder.fromHttpUrl(
                "http://content-collection-service/internal/contents/by-url")
            .queryParam("url", url)
            .toUriString();
    try {
      var response =
          restTemplate.exchange(requestUrl, HttpMethod.GET, internalRequest(), Long.class);
      return response.getBody();
    } catch (HttpClientErrorException.NotFound e) {
      return null;
    }
  }

  private Long findIdByUrlFallback(String url, Exception e) {
    log.warn("content-service 서킷 오픈 - findIdByUrl 스킵: {}", e.getMessage());
    return null;
  }

  public record ContentInfo(
      Long id, String title, String body, String url, String source, LocalDateTime publishedAt) {}

  public record PagedContentInfo(List<ContentInfo> contents, boolean hasNext, int totalPages) {}

  public record SaveContentRequest(
      String title,
      String body,
      String url,
      String source,
      String contentType,
      String contentSource) {}
}
