/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.sku.aissue.exception.CustomException;
import com.sku.aissue.exception.GlobalErrorCode;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

  private final RestTemplate restTemplate;

  @CircuitBreaker(name = "user", fallbackMethod = "getCredentialsFallback")
  public UserCredentialsDto getCredentials(String username) {
    try {
      return restTemplate.getForObject(
          "http://user-service/internal/users/{username}/credentials",
          UserCredentialsDto.class,
          username);
    } catch (HttpClientErrorException.NotFound e) {
      throw new UsernameNotFoundException(username);
    }
  }

  @SuppressWarnings("unused")
  private UserCredentialsDto getCredentialsFallback(String username, Throwable t) {
    if (t instanceof UsernameNotFoundException) {
      throw (UsernameNotFoundException) t;
    }
    log.error("user-service 호출 실패 (서킷 브레이커 fallback) - username: {}, error: {}", username, t.getMessage());
    throw new CustomException(GlobalErrorCode.SERVICE_UNAVAILABLE);
  }

  public record UserCredentialsDto(String username, String password, String role) {}
}