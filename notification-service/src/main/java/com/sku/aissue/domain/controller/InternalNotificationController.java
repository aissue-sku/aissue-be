/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.request.DirectNotificationRequest;
import com.sku.aissue.domain.dto.request.NotificationMatchRequest;
import com.sku.aissue.domain.dto.response.PopularKeywordResponse;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RequestMapping("/internal/notifications")
public interface InternalNotificationController {

  @PostMapping("/match")
  ResponseEntity<Void> matchAndNotify(@RequestBody NotificationMatchRequest request);

  @PostMapping("/direct")
  ResponseEntity<Void> sendDirect(@RequestBody DirectNotificationRequest request);

  @GetMapping("/popular-keywords")
  ResponseEntity<List<PopularKeywordResponse>> getPopularKeywords();

  @GetMapping("/subscriptions/{userId}")
  ResponseEntity<List<String>> getUserSubscribedKeywords(
      @org.springframework.web.bind.annotation.PathVariable String userId);
}
