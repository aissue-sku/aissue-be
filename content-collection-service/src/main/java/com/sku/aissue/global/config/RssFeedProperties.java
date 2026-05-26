/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rss")
public class RssFeedProperties {

  private List<Feed> feeds;

  @Getter
  @Setter
  public static class Feed {
    private String name;
    private String url;
  }
}
