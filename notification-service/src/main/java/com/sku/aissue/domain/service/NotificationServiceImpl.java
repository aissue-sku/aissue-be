/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sku.aissue.domain.dto.request.DirectNotificationRequest;
import com.sku.aissue.domain.dto.request.NotificationMatchRequest;
import com.sku.aissue.domain.dto.request.SubscribeRequest;
import com.sku.aissue.domain.dto.response.NotificationResponse;
import com.sku.aissue.domain.dto.response.PopularKeywordResponse;
import com.sku.aissue.domain.dto.response.SubscriptionResponse;
import com.sku.aissue.domain.entity.Notification;
import com.sku.aissue.domain.entity.Subscription;
import com.sku.aissue.domain.exception.NotificationErrorCode;
import com.sku.aissue.domain.repository.NotificationRepository;
import com.sku.aissue.domain.repository.SubscriptionRepository;
import com.sku.aissue.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

  private final SubscriptionRepository subscriptionRepository;
  private final NotificationRepository notificationRepository;

  @Override
  @Transactional
  public SubscriptionResponse subscribe(String userId, SubscribeRequest request) {
    if (subscriptionRepository.existsByUserIdAndKeyword(userId, request.getKeyword())) {
      throw new CustomException(NotificationErrorCode.ALREADY_SUBSCRIBED);
    }
    Subscription saved =
        subscriptionRepository.save(
            Subscription.builder().userId(userId).keyword(request.getKeyword()).build());
    log.info("구독 등록 - userId: {}, keyword: {}", userId, request.getKeyword());
    return SubscriptionResponse.builder()
        .keyword(saved.getKeyword())
        .subscribedAt(saved.getCreatedAt())
        .build();
  }

  @Override
  @Transactional
  public void unsubscribe(String userId, String keyword) {
    if (!subscriptionRepository.existsByUserIdAndKeyword(userId, keyword)) {
      throw new CustomException(NotificationErrorCode.SUBSCRIPTION_NOT_FOUND);
    }
    subscriptionRepository.deleteByUserIdAndKeyword(userId, keyword);
    log.info("구독 취소 - userId: {}, keyword: {}", userId, keyword);
  }

  @Override
  public List<SubscriptionResponse> getSubscriptions(String userId) {
    return subscriptionRepository.findByUserId(userId).stream()
        .map(
            s ->
                SubscriptionResponse.builder()
                    .keyword(s.getKeyword())
                    .subscribedAt(s.getCreatedAt())
                    .build())
        .toList();
  }

  @Override
  public List<NotificationResponse> getNotifications(String userId) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public long getUnreadCount(String userId) {
    return notificationRepository.countByUserIdAndIsReadFalse(userId);
  }

  @Override
  @Transactional
  public void markAsRead(String userId, Long notificationId) {
    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    if (!notification.getUserId().equals(userId)) {
      throw new CustomException(NotificationErrorCode.UNAUTHORIZED);
    }
    notification.markAsRead();
  }

  @Override
  @Transactional
  public void markAllAsRead(String userId) {
    notificationRepository.markAllAsRead(userId);
    log.info("전체 알림 읽음 처리 - userId: {}", userId);
  }

  @Override
  @Transactional
  public void deleteNotification(String userId, Long notificationId) {
    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    if (!notification.getUserId().equals(userId)) {
      throw new CustomException(NotificationErrorCode.UNAUTHORIZED);
    }
    notificationRepository.delete(notification);
    log.info("알림 삭제 - userId: {}, notificationId: {}", userId, notificationId);
  }

  @Override
  @Transactional
  public void deleteReadNotifications(String userId) {
    notificationRepository.deleteReadByUserId(userId);
    log.info("읽은 알림 삭제 - userId: {}", userId);
  }

  @Override
  @Transactional
  public void matchAndNotify(NotificationMatchRequest request) {
    if (request.getCards() == null || request.getCards().isEmpty()) return;

    for (NotificationMatchRequest.CardInfo card : request.getCards()) {
      if (card.getKeywords() == null || card.getKeywords().isEmpty()) continue;

      for (String keyword : card.getKeywords()) {
        List<Subscription> subscribers = subscriptionRepository.findByKeyword(keyword);
        for (Subscription sub : subscribers) {
          // 동일 URL + 키워드 중복 알림 방지
          if (card.getUrl() != null
              && notificationRepository.existsByUserIdAndKeywordAndUrl(
                  sub.getUserId(), keyword, card.getUrl())) {
            continue;
          }
          notificationRepository.save(
              Notification.builder()
                  .userId(sub.getUserId())
                  .keyword(keyword)
                  .title(card.getTitle())
                  .url(card.getUrl())
                  .contentId(card.getContentId())
                  .build());
        }
      }
    }
    log.info("알림 매칭 완료 - {}개 카드 처리", request.getCards().size());
  }

  @Override
  @Transactional
  public void sendDirect(DirectNotificationRequest request) {
    if (request.getUrl() != null
        && notificationRepository.existsByUserIdAndKeywordAndUrl(
            request.getUserId(), request.getKeyword(), request.getUrl())) {
      log.info("중복 알림 스킵 - userId: {}, url: {}", request.getUserId(), request.getUrl());
      return;
    }
    notificationRepository.save(
        Notification.builder()
            .userId(request.getUserId())
            .keyword(request.getKeyword())
            .title(request.getTitle())
            .url(request.getUrl())
            .contentId(request.getContentId())
            .build());
    log.info("시스템 알림 전송 - userId: {}, keyword: {}", request.getUserId(), request.getKeyword());
  }

  @Override
  public List<PopularKeywordResponse> getPopularKeywords() {
    return subscriptionRepository.findTopKeywordsBySubscriberCount(PageRequest.of(0, 3)).stream()
        .map(
            row ->
                PopularKeywordResponse.builder()
                    .keyword((String) row[0])
                    .subscriberCount((Long) row[1])
                    .build())
        .toList();
  }

  @Override
  public List<String> getAllSubscribedUserIds() {
    return subscriptionRepository.findAllDistinctUserIds();
  }

  private NotificationResponse toResponse(Notification n) {
    return NotificationResponse.builder()
        .id(n.getId())
        .keyword(n.getKeyword())
        .title(n.getTitle())
        .url(n.getUrl())
        .contentId(n.getContentId())
        .isRead(n.isRead())
        .createdAt(n.getCreatedAt())
        .build();
  }
}
