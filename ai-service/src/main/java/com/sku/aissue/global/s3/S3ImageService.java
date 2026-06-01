/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.s3;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sku.aissue.global.config.S3Properties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageService {

  private static final String KEY_PREFIX = "issue-cards/";
  private static final String CONTENT_TYPE = "image/png";

  private final S3Client s3Client;
  private final S3Properties s3Properties;

  public String upload(byte[] imageBytes) {
    String key = KEY_PREFIX + UUID.randomUUID() + ".png";

    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(s3Properties.getBucket())
            .key(key)
            .contentType(CONTENT_TYPE)
            .contentLength((long) imageBytes.length)
            .build();

    s3Client.putObject(request, RequestBody.fromBytes(imageBytes));

    String s3Url = buildS3Url(key);
    log.info("S3 이미지 업로드 완료 - url: {}", s3Url);
    return s3Url;
  }

  private String buildS3Url(String key) {
    return String.format(
        "https://%s.s3.%s.amazonaws.com/%s",
        s3Properties.getBucket(), s3Properties.getRegion(), key);
  }
}
