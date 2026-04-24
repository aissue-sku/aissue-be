/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

import com.sku.aissue.global.filter.GatewayHeaderFilter;

import lombok.RequiredArgsConstructor;

/**
 * user-service 보안 설정.
 *
 * <p>JWT 검증은 Gateway의 JwtAuthFilter가 담당하므로, user-service는 Gateway가 주입한 X-User-Name/X-User-Roles 헤더만
 * 신뢰합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final GatewayHeaderFilter gatewayHeaderFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(gatewayHeaderFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api/users/sign-up",
                        "/actuator/health")
                    .permitAll()
                    .requestMatchers(RegexRequestMatcher.regexMatcher(".*/dev(/.*)?"))
                    .hasRole("DEVELOPER")
                    .anyRequest()
                    .authenticated());

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
