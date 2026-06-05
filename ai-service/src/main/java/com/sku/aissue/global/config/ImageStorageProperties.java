/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "image.storage")
public class ImageStorageProperties {

  private String rootDir;

  private String urlPrefix;

  private String publicBaseUrl;
}
