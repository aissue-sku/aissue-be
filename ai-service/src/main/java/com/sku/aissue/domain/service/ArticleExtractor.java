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
  private static final Pattern ARC_GLOBAL_CONTENT =
      Pattern.compile("Fusion\\.globalContent\\s*=\\s*(\\{.*?\\});\\s*Fusion\\.", Pattern.DOTALL);

  private static final List<String> BODY_SELECTORS =
      List.of(
          "#dic_area",
          "#articleBodyContents",
          ".article_view",
          ".article-txt",
          ".article-text",
          "#news_body_id",
          ".article-body__content",
          ".article-body",
          ".article_body",
          ".ab_text",
          "#article_content",
          ".detail-body",
          ".text_area",
          ".article-content",
          ".news_txt",
          "article",
          "[role='main']",
          ".news-content",
          ".post-content",
          ".entry-content");

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
    String ogTitle = doc.select("meta[property=og:title]").attr("content");
    if (!ogTitle.isBlank()) return ogTitle.trim();
    return doc.title().trim();
  }

  private String extractBody(Document doc) {
    for (String selector : BODY_SELECTORS) {
      Element element = doc.selectFirst(selector);
      if (element != null) {
        String text = element.text().trim();
        if (text.length() >= MIN_PARAGRAPH_LENGTH) {
          return text;
        }
      }
    }

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

    String jsonLdBody = extractFromJsonLd(doc);
    if (jsonLdBody != null) return jsonLdBody;

    String nextDataBody = extractFromNextData(doc);
    if (nextDataBody != null) return nextDataBody;

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
