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

import com.sku.aissue.domain.dto.request.SubscriptionRequest;
import com.sku.aissue.domain.dto.response.NotificationResponse;
import com.sku.aissue.domain.dto.response.SubscriptionResponse;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "구독/알림", description = "키워드 구독 및 알림 API")
@RequestMapping("/api/subscriptions")
public interface SubscriptionController {

  @PostMapping
  @Operation(
      summary = "키워드 구독 등록",
      description = "관심 키워드를 등록합니다. 해당 키워드가 트렌딩에 등장하면 알림이 생성됩니다. **로그인 필요.**")
  ResponseEntity<BaseResponse<SubscriptionResponse>> subscribe(
      @RequestBody @Valid SubscriptionRequest request, @AuthenticationPrincipal String userId);

  @DeleteMapping("/{id}")
  @Operation(summary = "키워드 구독 취소", description = "구독 중인 키워드를 취소합니다. **로그인 필요.**")
  ResponseEntity<BaseResponse<Void>> unsubscribe(
      @PathVariable Long id, @AuthenticationPrincipal String userId);

  @GetMapping
  @Operation(summary = "내 구독 목록 조회", description = "구독 중인 키워드 목록을 반환합니다. **로그인 필요.**")
  ResponseEntity<BaseResponse<List<SubscriptionResponse>>> getSubscriptions(
      @AuthenticationPrincipal String userId);

  @GetMapping("/notifications")
  @Operation(summary = "내 알림 목록 조회", description = "키워드 구독으로 발생한 알림 목록을 반환합니다. **로그인 필요.**")
  ResponseEntity<BaseResponse<List<NotificationResponse>>> getNotifications(
      @AuthenticationPrincipal String userId);

  @PatchMapping("/notifications/{id}/read")
  @Operation(summary = "알림 읽음 처리", description = "알림을 읽음 상태로 변경합니다. **로그인 필요.**")
  ResponseEntity<BaseResponse<Void>> markAsRead(
      @PathVariable Long id, @AuthenticationPrincipal String userId);
}
