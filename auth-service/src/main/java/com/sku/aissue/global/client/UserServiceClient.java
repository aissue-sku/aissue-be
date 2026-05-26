/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

  private final RestTemplate restTemplate;

  public UserCredentialsDto getCredentials(String username) {
    try {
      return restTemplate.getForObject(
          "http://user-service/internal/users/{username}/credentials",
          UserCredentialsDto.class,
          username);
    } catch (HttpClientErrorException.NotFound e) {
      throw new UsernameNotFoundException(username);
    } catch (Exception e) {
      log.error("user-service 인증 정보 조회 실패 - username: {}, error: {}", username, e.getMessage());
      throw new UsernameNotFoundException(username);
    }
  }

  public record UserCredentialsDto(String username, String password, String role) {}
}
