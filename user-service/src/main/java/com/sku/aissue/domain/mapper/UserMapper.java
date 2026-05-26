/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.mapper;

import org.springframework.stereotype.Component;

import com.sku.aissue.domain.dto.request.SignUpRequest;
import com.sku.aissue.domain.dto.response.UserDetailResponse;
import com.sku.aissue.domain.dto.response.UserResponse;
import com.sku.aissue.domain.entity.CharacterItem;
import com.sku.aissue.domain.entity.Role;
import com.sku.aissue.domain.entity.User;
import com.sku.aissue.domain.entity.UserCharacter;

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

  public UserResponse toUserResponse(User user, UserCharacter character) {
    String color =
        character != null && character.getColor() != null
            ? character.getColor().name()
            : CharacterItem.AISSUE.name();
    return UserResponse.builder()
        .userId(user.getId())
        .username(user.getUsername())
        .name(user.getName())
        .color(color)
        .points(user.getPoints())
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
