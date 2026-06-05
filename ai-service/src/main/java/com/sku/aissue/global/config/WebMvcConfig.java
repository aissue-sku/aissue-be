/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.config;

import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final ImageStorageProperties properties;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String pattern = trimSlash(properties.getUrlPrefix()) + "/**";
    String location = "file:" + Paths.get(properties.getRootDir()).toAbsolutePath() + "/";

    registry.addResourceHandler(pattern).addResourceLocations(location);
  }

  private String trimSlash(String value) {
    if (value == null || value.isEmpty()) return "";
    String normalized = value.startsWith("/") ? value : "/" + value;
    return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
  }
}
