/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

import com.sku.aissue.global.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    configureFilters(http);
    configureExceptionHandling(http);
    configureAuthorization(http);
    return http.build();
  }

  /** 필터와 기본 설정 */
  private void configureFilters(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
  }

  /** 예외 처리: 인증 실패와 권한 부족 처리 */
  private void configureExceptionHandling(HttpSecurity http) throws Exception {
    http.exceptionHandling(
        e ->
            e.authenticationEntryPoint(this::handleAuthException)
                .accessDeniedHandler(this::handleAccessDenied));
  }

  private void handleAuthException(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response
        .getWriter()
        .write("{\"success\": false, \"code\": 401, \"message\": \"JWT 토큰이 없거나 유효하지 않습니다.\"}");
    log.warn("인증 실패: {} {}", request.getMethod(), request.getRequestURI());
  }

  private void handleAccessDenied(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json;charset=UTF-8");

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = "anonymous";

    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
      Object principal = auth.getPrincipal();
      if (principal instanceof String str) {
        username = str;
      }
    }

    response
        .getWriter()
        .write("{\"success\": false, \"code\": 403, \"message\": \"접근 권한이 없습니다.\"}");
    log.warn("권한 부족: {} {}, username={}", request.getMethod(), request.getRequestURI(), username);
  }

  /** 권한 설정 */
  private void configureAuthorization(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
        auth ->
            auth
                // 인증 없이 접근 가능한 엔드포인트
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/",
                    "/api/auths/login",
                    "/api/auths/refresh",
                    "/actuator/health")
                .permitAll()

                // 권한 필요 엔드포인트
                .requestMatchers(RegexRequestMatcher.regexMatcher(".*/admin(/.*)?"))
                .hasRole("ADMIN")
                .requestMatchers(RegexRequestMatcher.regexMatcher(".*/dev(/.*)?"))
                .hasRole("DEVELOPER")
                .anyRequest()
                .authenticated());
  }

  /** 비밀번호 인코더 Bean */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** 인증 관리자 Bean */
  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }
}
