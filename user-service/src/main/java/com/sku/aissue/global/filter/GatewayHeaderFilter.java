/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * Gateway에서 JWT 검증 후 주입하는 X-User-Name, X-User-Roles 헤더를 읽어 SecurityContextHolder에 인증 정보를 설정하는 필터.
 *
 * <p>Gateway의 JwtAuthFilter가 토큰을 검증하므로 user-service는 별도의 JWT 파싱 없이 헤더만 신뢰합니다.
 */
@Slf4j
@Component
public class GatewayHeaderFilter extends OncePerRequestFilter {

  private static final String HEADER_USERNAME = "X-User-Name";
  private static final String HEADER_ROLES = "X-User-Roles";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String username = request.getHeader(HEADER_USERNAME);
    String rolesHeader = request.getHeader(HEADER_ROLES);

    if (username != null && !username.isBlank()) {
      Collection<GrantedAuthority> authorities = parseAuthorities(rolesHeader);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(username, null, authorities);

      SecurityContextHolder.getContext().setAuthentication(authentication);
      log.debug("Gateway 헤더 인증 설정 - username: {}", username);
    }

    filterChain.doFilter(request, response);
  }

  private Collection<GrantedAuthority> parseAuthorities(String rolesHeader) {
    if (rolesHeader == null || rolesHeader.isBlank()) {
      return java.util.Collections.emptyList();
    }

    return Arrays.stream(rolesHeader.split(","))
        .map(String::trim)
        .filter(role -> !role.isBlank())
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }
}
