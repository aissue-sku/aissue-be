/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.request.AddPointsRequest;
import com.sku.aissue.domain.dto.response.UserCredentialsResponse;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RequestMapping("/internal/users")
public interface InternalUserController {

  @GetMapping("/{username}/credentials")
  ResponseEntity<UserCredentialsResponse> getCredentials(@PathVariable String username);

  @PostMapping("/{userId}/points")
  ResponseEntity<Void> addPoints(@PathVariable Long userId, @RequestBody AddPointsRequest request);

  @PostMapping("/by-username/{username}/points")
  ResponseEntity<Void> addPointsByUsername(
      @PathVariable String username, @RequestBody AddPointsRequest request);
}
