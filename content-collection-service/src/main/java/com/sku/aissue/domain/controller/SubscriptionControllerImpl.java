/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.request.SubscriptionRequest;
import com.sku.aissue.domain.dto.response.NotificationResponse;
import com.sku.aissue.domain.dto.response.SubscriptionResponse;
import com.sku.aissue.domain.service.SubscriptionService;
import com.sku.aissue.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SubscriptionControllerImpl implements SubscriptionController {

  private final SubscriptionService subscriptionService;

  @Override
  public ResponseEntity<BaseResponse<SubscriptionResponse>> subscribe(
      SubscriptionRequest request, @AuthenticationPrincipal String userId) {
    return ResponseEntity.ok(
        BaseResponse.success(subscriptionService.subscribe(userId, request.getKeyword())));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> unsubscribe(
      Long id, @AuthenticationPrincipal String userId) {
    subscriptionService.unsubscribe(userId, id);
    return ResponseEntity.ok(BaseResponse.success(null));
  }

  @Override
  public ResponseEntity<BaseResponse<List<SubscriptionResponse>>> getSubscriptions(
      @AuthenticationPrincipal String userId) {
    return ResponseEntity.ok(BaseResponse.success(subscriptionService.getSubscriptions(userId)));
  }

  @Override
  public ResponseEntity<BaseResponse<List<NotificationResponse>>> getNotifications(
      @AuthenticationPrincipal String userId) {
    return ResponseEntity.ok(BaseResponse.success(subscriptionService.getNotifications(userId)));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> markAsRead(
      Long id, @AuthenticationPrincipal String userId) {
    subscriptionService.markAsRead(userId, id);
    return ResponseEntity.ok(BaseResponse.success(null));
  }
}
