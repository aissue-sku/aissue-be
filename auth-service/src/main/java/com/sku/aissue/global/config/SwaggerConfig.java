/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.config;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

  private static final String SECURITY_SCHEME_NAME = "bearerAuth";

  @Bean
  public OpenAPI customOpenAPI() {

    return new OpenAPI()

        // 게이트웨이 경유로 호출되도록 서버 URL 고정 (Swagger UI가 컨테이너 내부 IP를 사용하는 문제 방지)
        .servers(List.of(new Server().url("/").description("Gateway")))

        // 전역 JWT 보안 설정
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))

        // SecurityScheme 정의
        .components(
            new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))

        // API 기본 정보
        .info(
            new Info()
                .title("📸 AIssue 인증/인가 관련 API 명세서")
                .version("v1.1.0")
                .description(
                    """
                    ## 주의사항
                    - Access Token은 Authorization 헤더(Bearer)로 전달합니다.
                    - Refresh Token은 HttpOnly 쿠키로 관리됩니다.
                    - 토큰 만료 시 /api/auths/refresh로 갱신합니다.

                    ## 문의
                    - 기술 문의: unijun0109@gmail.com
                    """)
                .contact(new Contact().name("AIssue").email("unijun0109@gmail.com")));
  }

  @Bean
  public GroupedOpenApi apiGroup() {
    return GroupedOpenApi.builder().group("api").pathsToMatch("/api/**").build();
  }
}
