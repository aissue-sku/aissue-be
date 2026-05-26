/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.collector;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.sku.aissue.domain.dto.CollectedContentDto;
import com.sku.aissue.domain.entity.ContentSource;
import com.sku.aissue.domain.entity.ContentType;
import com.sku.aissue.global.config.RssFeedProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssCollector implements ContentCollector {

  private final RssFeedProperties rssFeedProperties;

  @Override
  public List<CollectedContentDto> collect() {
    if (rssFeedProperties.getFeeds() == null) {
      return Collections.emptyList();
    }

    return rssFeedProperties.getFeeds().stream().flatMap(feed -> fetchFeed(feed).stream()).toList();
  }

  @Override
  public ContentSource getSource() {
    return ContentSource.RSS;
  }

  private List<CollectedContentDto> fetchFeed(RssFeedProperties.Feed feedConfig) {
    try {
      SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(feedConfig.getUrl())));

      return feed.getEntries().stream().map(entry -> toDto(entry, feedConfig.getName())).toList();

    } catch (Exception e) {
      log.error("RSS 수집 실패 - source: {}, error: {}", feedConfig.getName(), e.getMessage());
      return Collections.emptyList();
    }
  }

  private CollectedContentDto toDto(SyndEntry entry, String sourceName) {
    String body =
        entry.getContents().isEmpty()
            ? (entry.getDescription() != null ? entry.getDescription().getValue() : null)
            : entry.getContents().get(0).getValue();

    return CollectedContentDto.builder()
        .title(entry.getTitle())
        .body(stripHtmlTags(body))
        .url(entry.getLink())
        .source(sourceName)
        .contentType(ContentType.NEWS)
        .contentSource(ContentSource.RSS)
        .publishedAt(toLocalDateTime(entry))
        .build();
  }

  private String stripHtmlTags(String text) {
    if (text == null) return null;
    return text.replaceAll("<[^>]+>", "").trim();
  }

  private LocalDateTime toLocalDateTime(SyndEntry entry) {
    if (entry.getPublishedDate() != null) {
      return entry.getPublishedDate().toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }
    if (entry.getUpdatedDate() != null) {
      return entry.getUpdatedDate().toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }
    return LocalDateTime.now();
  }
}
