/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.mapper;

import org.springframework.stereotype.Component;

import com.sku.aissue.domain.dto.request.SignUpRequest;
import com.sku.aissue.domain.dto.response.UserDetailResponse;
import com.sku.aissue.domain.dto.response.UserResponse;
import com.sku.aissue.domain.entity.Role;
import com.sku.aissue.domain.entity.User;

@Component
public class UserMapper {

  public User toUser(SignUpRequest request, String encodedPassword) {
    return User.builder()
        .username(request.getUsername())
        .password(encodedPassword)
        .name(request.getName())
        .role(Role.ROLE_USER)
        .build();
  }

  public UserResponse toUserResponse(User user) {
    return UserResponse.builder()
        .userId(user.getId())
        .username(user.getUsername())
        .name(user.getName())
        .build();
  }

  public UserDetailResponse toUserDetailResponse(User user) {
    return UserDetailResponse.builder()
        .userId(user.getId())
        .username(user.getUsername())
        .name(user.getName())
        .role(user.getRole().toString())
        .build();
  }
}
