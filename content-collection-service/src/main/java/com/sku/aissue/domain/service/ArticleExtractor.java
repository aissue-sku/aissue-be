/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sku.aissue.domain.exception.AnalysisErrorCode;
import com.sku.aissue.exception.CustomException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ArticleExtractor {

  private static final int CONNECT_TIMEOUT_MS = 5_000;
  private static final int MIN_PARAGRAPH_LENGTH = 20;
  private static final int MIN_ARTICLE_BODY_LENGTH = 200;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  // Arc Publishing CMS (조선일보, 중앙일보 등)
  private static final Pattern ARC_GLOBAL_CONTENT =
      Pattern.compile("Fusion\\.globalContent\\s*=\\s*(\\{.*?\\});\\s*Fusion\\.", Pattern.DOTALL);

  // 뉴스 사이트별 본문 CSS 셀렉터 (우선순위 순)
  private static final List<String> BODY_SELECTORS =
      List.of(
          // 네이버 뉴스
          "#dic_area",
          "#articleBodyContents",
          // 다음 뉴스
          ".article_view",
          // 연합뉴스
          ".article-txt",
          // 한겨레
          ".article-text",
          // 조선일보 (구/신 셀렉터)
          "#news_body_id",
          ".article-body__content",
          ".article-body",
          // 중앙일보
          ".article_body",
          ".ab_text",
          // 동아일보
          "#article_content",
          // KBS
          ".detail-body",
          // SBS
          ".text_area",
          // YTN
          ".article-content",
          // MBC
          ".news_txt",
          // 일반 시맨틱 태그
          "article",
          "[role='main']",
          ".news-content",
          ".post-content",
          ".entry-content");

  /**
   * URL로부터 기사 본문을 추출합니다.
   *
   * @throws CustomException URL 형식 오류, 접근 불가, 기사 본문 없음
   */
  public ExtractedArticle extract(String url) {
    validateUrl(url);

    try {
      Document doc =
          Jsoup.connect(url)
              .userAgent("Mozilla/5.0 (compatible; AIssueBot/1.0; +https://github.com/uni-j-uni)")
              .timeout(CONNECT_TIMEOUT_MS)
              .get();

      String title = extractTitle(doc);
      String body = extractBody(doc);

      if (body == null || body.isBlank()) {
        log.warn("기사 본문 추출 실패 - url: {}", url);
        throw new CustomException(AnalysisErrorCode.NOT_ARTICLE_URL);
      }

      log.debug("기사 추출 완료 - url: {}, bodyLength: {}", url, body.length());
      return new ExtractedArticle(title, body);

    } catch (CustomException e) {
      throw e;
    } catch (HttpStatusException e) {
      log.warn("URL 접근 실패 - url: {}, status: {}", url, e.getStatusCode());
      throw new CustomException(AnalysisErrorCode.URL_NOT_ACCESSIBLE);
    } catch (IOException e) {
      log.warn("URL 접근 실패 - url: {}, error: {}", url, e.getMessage());
      throw new CustomException(AnalysisErrorCode.URL_NOT_ACCESSIBLE);
    }
  }

  private void validateUrl(String url) {
    if (url == null || url.isBlank()) {
      throw new CustomException(AnalysisErrorCode.INVALID_URL);
    }
    try {
      URL parsed = new URL(url);
      String protocol = parsed.getProtocol();
      if (!"http".equals(protocol) && !"https".equals(protocol)) {
        throw new CustomException(AnalysisErrorCode.INVALID_URL);
      }
    } catch (MalformedURLException e) {
      throw new CustomException(AnalysisErrorCode.INVALID_URL);
    }
  }

  private String extractTitle(Document doc) {
    // og:title 메타 태그 우선
    String ogTitle = doc.select("meta[property=og:title]").attr("content");
    if (!ogTitle.isBlank()) return ogTitle.trim();

    return doc.title().trim();
  }

  private String extractBody(Document doc) {
    // 1. 뉴스 사이트별 셀렉터 시도
    for (String selector : BODY_SELECTORS) {
      Element element = doc.selectFirst(selector);
      if (element != null) {
        String text = element.text().trim();
        if (text.length() >= MIN_PARAGRAPH_LENGTH) {
          return text;
        }
      }
    }

    // 2. 셀렉터 실패 시 <p> 태그 중 긴 문단들을 이어붙임
    Elements paragraphs = doc.select("p");
    StringBuilder bodyBuilder = new StringBuilder();
    for (Element p : paragraphs) {
      String text = p.text().trim();
      if (text.length() >= MIN_PARAGRAPH_LENGTH) {
        bodyBuilder.append(text).append(" ");
      }
    }
    String fallback = bodyBuilder.toString().trim();
    if (!fallback.isEmpty()) return fallback;

    // 3. JSON-LD structured data (표준 NewsArticle 스키마의 articleBody)
    String jsonLdBody = extractFromJsonLd(doc);
    if (jsonLdBody != null) return jsonLdBody;

    // 4. Next.js __NEXT_DATA__ JSON (React SPA 사이트 대응)
    String nextDataBody = extractFromNextData(doc);
    if (nextDataBody != null) return nextDataBody;

    // 5. Arc Publishing CMS — Fusion.globalContent (조선일보 등)
    return extractFromArcCms(doc);
  }

  private String extractFromJsonLd(Document doc) {
    for (Element script : doc.select("script[type=application/ld+json]")) {
      try {
        JsonNode node = OBJECT_MAPPER.readTree(script.data());
        String body = findJsonField(node, "articleBody", 0);
        if (body != null && body.length() >= MIN_PARAGRAPH_LENGTH) return body;
      } catch (Exception ignored) {
      }
    }
    return null;
  }

  private String extractFromNextData(Document doc) {
    Element el = doc.selectFirst("script#__NEXT_DATA__");
    if (el == null) return null;
    try {
      JsonNode root = OBJECT_MAPPER.readTree(el.data());
      // articleBody 우선, 없으면 충분히 긴 content/body 필드 탐색
      String body = findJsonField(root, "articleBody", 0);
      if (body != null && body.length() >= MIN_PARAGRAPH_LENGTH) return body;
      body = findLongJsonTextField(root, 0);
      if (body != null) return body;
    } catch (Exception ignored) {
    }
    return null;
  }

  private String findJsonField(JsonNode node, String fieldName, int depth) {
    if (depth > 10 || node == null || node.isNull() || node.isTextual()) return null;
    if (node.isObject()) {
      JsonNode target = node.path(fieldName);
      if (target.isTextual() && !target.asText().isBlank()) return target.asText();
      Iterator<JsonNode> vals = node.elements();
      while (vals.hasNext()) {
        String r = findJsonField(vals.next(), fieldName, depth + 1);
        if (r != null) return r;
      }
    }
    if (node.isArray()) {
      for (JsonNode child : node) {
        String r = findJsonField(child, fieldName, depth + 1);
        if (r != null) return r;
      }
    }
    return null;
  }

  // Next.js pageProps에서 긴 문자열 필드(본문)를 찾음
  private String findLongJsonTextField(JsonNode node, int depth) {
    if (depth > 8 || node == null || node.isNull()) return null;
    if (node.isObject()) {
      for (String key : List.of("content", "body", "text", "description")) {
        JsonNode field = node.path(key);
        if (field.isTextual() && field.asText().length() >= MIN_ARTICLE_BODY_LENGTH) {
          return field.asText();
        }
      }
      Iterator<JsonNode> vals = node.elements();
      while (vals.hasNext()) {
        String r = findLongJsonTextField(vals.next(), depth + 1);
        if (r != null) return r;
      }
    }
    if (node.isArray()) {
      for (JsonNode child : node) {
        String r = findLongJsonTextField(child, depth + 1);
        if (r != null) return r;
      }
    }
    return null;
  }

  // Arc Publishing CMS: Fusion.globalContent.content_elements[].content (type=="text")
  private String extractFromArcCms(Document doc) {
    for (Element script : doc.select("script:not([src])")) {
      Matcher m = ARC_GLOBAL_CONTENT.matcher(script.data());
      if (!m.find()) continue;
      try {
        JsonNode root = OBJECT_MAPPER.readTree(m.group(1));
        JsonNode elements = root.path("content_elements");
        if (!elements.isArray()) continue;
        StringBuilder sb = new StringBuilder();
        for (JsonNode el : elements) {
          if ("text".equals(el.path("type").asText())) {
            String content = el.path("content").asText();
            if (!content.isBlank()) sb.append(content).append(" ");
          }
        }
        String body = sb.toString().trim();
        if (body.length() >= MIN_PARAGRAPH_LENGTH) return body;
      } catch (Exception ignored) {
      }
    }
    return null;
  }

  public record ExtractedArticle(String title, String body) {
    public boolean isEmpty() {
      return (title == null || title.isBlank()) && (body == null || body.isBlank());
    }

    public static ExtractedArticle empty() {
      return new ExtractedArticle(null, null);
    }
  }
}
