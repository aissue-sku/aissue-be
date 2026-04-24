/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sku.aissue.domain.user.entity.User;
import com.sku.aissue.domain.user.exception.UserErrorCode;
import com.sku.aissue.domain.user.repository.UserRepository;
import com.sku.aissue.exception.CustomException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

    return new CustomUserDetails(user);
  }
}
