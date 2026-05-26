/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sku.aissue.global.client.UserServiceClient;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserServiceClient userServiceClient;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserServiceClient.UserCredentialsDto credentials = userServiceClient.getCredentials(username);
    return new CustomUserDetails(credentials);
  }
}
