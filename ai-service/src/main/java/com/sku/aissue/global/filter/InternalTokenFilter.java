/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

  private static final String HEADER = "X-Internal-Token";
  private static final String INTERNAL_PATH = "/internal/";

  @Value("${internal.token}")
  private String internalToken;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (!request.getRequestURI().startsWith(INTERNAL_PATH)) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = request.getHeader(HEADER);
    if (!internalToken.equals(token)) {
      log.warn(
          "Internal API 인증 실패 - uri: {}, remoteAddr: {}",
          request.getRequestURI(),
          request.getRemoteAddr());
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid internal token");
      return;
    }

    filterChain.doFilter(request, response);
  }
}
