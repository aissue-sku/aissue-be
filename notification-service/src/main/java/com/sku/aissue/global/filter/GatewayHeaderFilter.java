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
