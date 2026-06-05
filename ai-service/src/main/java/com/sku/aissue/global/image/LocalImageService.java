/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.sku.aissue.global.config.ImageStorageProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalImageService {

  private static final String FILE_EXTENSION = ".png";

  private final ImageStorageProperties properties;

  @PostConstruct
  void init() throws IOException {
    Path rootPath = Paths.get(properties.getRootDir());
    Files.createDirectories(rootPath);
    log.info("이슈 카드 이미지 저장 디렉토리 준비 완료 - path: {}", rootPath.toAbsolutePath());
  }

  public String upload(byte[] imageBytes) {
    String filename = UUID.randomUUID() + FILE_EXTENSION;
    Path target = Paths.get(properties.getRootDir(), filename);

    try {
      Files.write(target, imageBytes);
    } catch (IOException e) {
      throw new IllegalStateException("이미지 파일 저장 실패", e);
    }

    String url = buildPublicUrl(filename);
    log.info("이미지 저장 완료 - url: {}", url);
    return url;
  }

  private String buildPublicUrl(String filename) {
    String baseUrl = stripTrailingSlash(properties.getPublicBaseUrl());
    String prefix = ensureLeadingSlash(stripTrailingSlash(properties.getUrlPrefix()));
    return baseUrl + prefix + "/" + filename;
  }

  private String stripTrailingSlash(String value) {
    if (value == null || value.isEmpty()) return "";
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private String ensureLeadingSlash(String value) {
    if (value == null || value.isEmpty()) return "";
    return value.startsWith("/") ? value : "/" + value;
  }
}
