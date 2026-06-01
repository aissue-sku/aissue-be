/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.sku.aissue.global.filter.GatewayHeaderFilter;
import com.sku.aissue.global.filter.InternalTokenFilter;

import lombok.RequiredArgsConstructor;

/**
 * content-collection-service 보안 설정.
 *
 * <p>JWT 검증은 Gateway의 JwtAuthFilter가 담당하므로, content-collection-service는 Gateway가 주입한
 * X-User-Name/X-User-Roles 헤더만 신뢰합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final GatewayHeaderFilter gatewayHeaderFilter;
  private final InternalTokenFilter internalTokenFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(internalTokenFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(gatewayHeaderFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "**")
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    return http.build();
  }
}
