/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// com.sku.aissue 하위 전체 스캔 → common의 global.* 빈들도 자동 등록
@EnableJpaAuditing
@SpringBootApplication
public class AuthServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuthServiceApplication.class, args);
  }
}
