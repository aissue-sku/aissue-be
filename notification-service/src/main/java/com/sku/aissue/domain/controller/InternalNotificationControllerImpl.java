/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.request.DirectNotificationRequest;
import com.sku.aissue.domain.dto.request.NotificationMatchRequest;
import com.sku.aissue.domain.dto.response.PopularKeywordResponse;
import com.sku.aissue.domain.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class InternalNotificationControllerImpl implements InternalNotificationController {

  private final NotificationService notificationService;

  @Override
  public ResponseEntity<Void> matchAndNotify(NotificationMatchRequest request) {
    notificationService.matchAndNotify(request);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> sendDirect(DirectNotificationRequest request) {
    notificationService.sendDirect(request);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<List<PopularKeywordResponse>> getPopularKeywords() {
    return ResponseEntity.ok(notificationService.getPopularKeywords());
  }
}
