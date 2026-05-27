/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.request.SubscribeRequest;
import com.sku.aissue.domain.dto.response.NotificationResponse;
import com.sku.aissue.domain.dto.response.SubscriptionResponse;
import com.sku.aissue.domain.service.NotificationService;
import com.sku.aissue.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class NotificationControllerImpl implements NotificationController {

  private final NotificationService notificationService;

  @Override
  public ResponseEntity<BaseResponse<SubscriptionResponse>> subscribe(
      String userId, SubscribeRequest request) {
    SubscriptionResponse response = notificationService.subscribe(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BaseResponse.success(201, "키워드 구독이 완료되었습니다.", response));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> unsubscribe(String userId, String keyword) {
    notificationService.unsubscribe(userId, keyword);
    return ResponseEntity.ok(BaseResponse.success(200, "키워드 구독이 취소되었습니다.", null));
  }

  @Override
  public ResponseEntity<BaseResponse<List<SubscriptionResponse>>> getSubscriptions(String userId) {
    List<SubscriptionResponse> subscriptions = notificationService.getSubscriptions(userId);
    return ResponseEntity.ok(BaseResponse.success(200, "구독 키워드 목록 조회 성공", subscriptions));
  }

  @Override
  public ResponseEntity<BaseResponse<List<NotificationResponse>>> getNotifications(String userId) {
    List<NotificationResponse> notifications = notificationService.getNotifications(userId);
    return ResponseEntity.ok(BaseResponse.success(200, "알림 목록 조회 성공", notifications));
  }

  @Override
  public ResponseEntity<BaseResponse<Long>> getUnreadCount(String userId) {
    long count = notificationService.getUnreadCount(userId);
    return ResponseEntity.ok(BaseResponse.success(200, "읽지 않은 알림 수 조회 성공", count));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> markAsRead(String userId, Long id) {
    notificationService.markAsRead(userId, id);
    return ResponseEntity.ok(BaseResponse.success(200, "알림 읽음 처리 완료", null));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> markAllAsRead(String userId) {
    notificationService.markAllAsRead(userId);
    return ResponseEntity.ok(BaseResponse.success(200, "전체 알림 읽음 처리 완료", null));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> deleteNotification(String userId, Long id) {
    notificationService.deleteNotification(userId, id);
    return ResponseEntity.ok(BaseResponse.success(200, "알림 삭제 완료", null));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> deleteReadNotifications(String userId) {
    notificationService.deleteReadNotifications(userId);
    return ResponseEntity.ok(BaseResponse.success(200, "읽은 알림 삭제 완료", null));
  }
}
