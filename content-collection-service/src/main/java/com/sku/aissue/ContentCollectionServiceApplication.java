/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@EnableCaching
@EnableAsync
@SpringBootApplication
public class ContentCollectionServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ContentCollectionServiceApplication.class, args);
  }
}
