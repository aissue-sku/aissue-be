/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  private static final String SECURITY_SCHEME_NAME = "bearerAuth";

  @Bean
  public OpenAPI customOpenAPI() {

    return new OpenAPI()

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
                .title("📸 AIssue 사용자 관련 API 명세서")
                .version("v1.1.0")
                .description(
                    """
                    ## 주의사항
                    - 모든 API는 Authorization 헤더(Bearer)에 Access Token이 필요합니다.
                    - 회원가입(/api/users/sign-up)은 인증 없이 호출 가능합니다.

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
