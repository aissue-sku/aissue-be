/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.util.List;

import com.sku.aissue.domain.dto.request.DirectNotificationRequest;
import com.sku.aissue.domain.dto.request.NotificationMatchRequest;
import com.sku.aissue.domain.dto.request.SubscribeRequest;
import com.sku.aissue.domain.dto.response.NotificationResponse;
import com.sku.aissue.domain.dto.response.SubscriptionResponse;

public interface NotificationService {

  SubscriptionResponse subscribe(String userId, SubscribeRequest request);

  void unsubscribe(String userId, String keyword);

  List<SubscriptionResponse> getSubscriptions(String userId);

  List<NotificationResponse> getNotifications(String userId);

  long getUnreadCount(String userId);

  void markAsRead(String userId, Long notificationId);

  void markAllAsRead(String userId);

  void deleteNotification(String userId, Long notificationId);

  void deleteReadNotifications(String userId);

  void matchAndNotify(NotificationMatchRequest request);

  void sendDirect(DirectNotificationRequest request);
}
