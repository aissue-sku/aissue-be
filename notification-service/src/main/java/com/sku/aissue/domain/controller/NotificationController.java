/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.request.SubscribeRequest;
import com.sku.aissue.domain.dto.response.NotificationResponse;
import com.sku.aissue.domain.dto.response.SubscriptionResponse;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "알림", description = "구독 키워드 및 알림 관련 API")
@RequestMapping("/api/notifications")
public interface NotificationController {

  @Operation(summary = "키워드 구독")
  @PostMapping("/subscriptions")
  ResponseEntity<BaseResponse<SubscriptionResponse>> subscribe(
      @AuthenticationPrincipal String userId, @RequestBody @Valid SubscribeRequest request);

  @Operation(summary = "키워드 구독 취소")
  @DeleteMapping("/subscriptions/{keyword}")
  ResponseEntity<BaseResponse<Void>> unsubscribe(
      @AuthenticationPrincipal String userId, @PathVariable String keyword);

  @Operation(summary = "내 구독 키워드 목록 조회")
  @GetMapping("/subscriptions")
  ResponseEntity<BaseResponse<List<SubscriptionResponse>>> getSubscriptions(
      @AuthenticationPrincipal String userId);

  @Operation(summary = "내 알림 목록 조회")
  @GetMapping
  ResponseEntity<BaseResponse<List<NotificationResponse>>> getNotifications(
      @AuthenticationPrincipal String userId);

  @Operation(summary = "읽지 않은 알림 수 조회")
  @GetMapping("/unread-count")
  ResponseEntity<BaseResponse<Long>> getUnreadCount(@AuthenticationPrincipal String userId);

  @Operation(summary = "알림 읽음 처리")
  @PatchMapping("/{id}/read")
  ResponseEntity<BaseResponse<Void>> markAsRead(
      @AuthenticationPrincipal String userId, @PathVariable Long id);

  @Operation(summary = "전체 알림 읽음 처리")
  @PatchMapping("/read-all")
  ResponseEntity<BaseResponse<Void>> markAllAsRead(@AuthenticationPrincipal String userId);

  @Operation(summary = "읽은 알림 전체 삭제")
  @DeleteMapping("/read")
  ResponseEntity<BaseResponse<Void>> deleteReadNotifications(
      @AuthenticationPrincipal String userId);
}
