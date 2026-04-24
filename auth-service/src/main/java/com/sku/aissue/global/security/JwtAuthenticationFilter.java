/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

import com.sku.aissue.global.jwt.JwtProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if ("/error".equals(request.getRequestURI())) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = jwtProvider.extractAccessToken(request);

    if (token == null || token.trim().isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      // 서명·만료·블랙리스트·타입을 한 번의 파싱으로 검증
      Claims claims = jwtProvider.validateAndGetAccessClaims(token);
      if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        String username = claims.getSubject();
        String authStr = claims.get("auth", String.class);

        // JWT claims에서 직접 권한 파싱 — DB 조회 불필요
        List<GrantedAuthority> authorities =
            (authStr == null || authStr.isBlank())
                ? Collections.emptyList()
                : Arrays.stream(authStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (JwtException | IllegalArgumentException e) {
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }
}
