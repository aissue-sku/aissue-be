/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.request.AddPointsRequest;
import com.sku.aissue.domain.dto.response.UserCredentialsResponse;
import com.sku.aissue.domain.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class InternalUserControllerImpl implements InternalUserController {

  private final UserService userService;

  @Override
  public ResponseEntity<UserCredentialsResponse> getCredentials(String username) {
    return ResponseEntity.ok(userService.getUserCredentials(username));
  }

  @Override
  public ResponseEntity<Void> addPoints(Long userId, AddPointsRequest request) {
    userService.addPoints(userId, request);
    return ResponseEntity.ok().build();
  }
}
