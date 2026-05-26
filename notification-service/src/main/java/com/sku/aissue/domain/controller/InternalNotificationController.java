/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.request.DirectNotificationRequest;
import com.sku.aissue.domain.dto.request.NotificationMatchRequest;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RequestMapping("/internal/notifications")
public interface InternalNotificationController {

  @PostMapping("/match")
  ResponseEntity<Void> matchAndNotify(@RequestBody NotificationMatchRequest request);

  @PostMapping("/direct")
  ResponseEntity<Void> sendDirect(@RequestBody DirectNotificationRequest request);
}
