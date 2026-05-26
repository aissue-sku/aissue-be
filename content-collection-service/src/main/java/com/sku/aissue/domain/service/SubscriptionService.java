/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sku.aissue.domain.dto.response.NotificationResponse;
import com.sku.aissue.domain.dto.response.SubscriptionResponse;
import com.sku.aissue.domain.entity.Notification;
import com.sku.aissue.domain.entity.Subscription;
import com.sku.aissue.domain.entity.TrendingSnapshot;
import com.sku.aissue.domain.repository.NotificationRepository;
import com.sku.aissue.domain.repository.SubscriptionRepository;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.exception.GlobalErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

  private final SubscriptionRepository subscriptionRepository;
  private final NotificationRepository notificationRepository;

  @Transactional
  public SubscriptionResponse subscribe(String userId, String keyword) {
    log.info("키워드 구독 요청 시작 - userId: {}, keyword: {}", userId, keyword);

    if (subscriptionRepository.existsByUserIdAndKeyword(userId, keyword)) {
      log.info("키워드 구독 실패 - 이미 구독 중: {}", keyword);
      throw new CustomException(GlobalErrorCode.DUPLICATE_RESOURCE);
    }

    Subscription subscription =
        subscriptionRepository.save(
            Subscription.builder()
                .userId(userId)
                .keyword(keyword)
                .createdAt(LocalDateTime.now())
                .build());

    log.info("키워드 구독 성공 - userId: {}, keyword: {}", userId, keyword);
    return SubscriptionResponse.from(subscription);
  }

  @Transactional
  public void unsubscribe(String userId, Long subscriptionId) {
    log.info("키워드 구독 취소 요청 시작 - userId: {}, subscriptionId: {}", userId, subscriptionId);

    Subscription subscription =
        subscriptionRepository
            .findById(subscriptionId)
            .orElseThrow(() -> new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    if (!subscription.getUserId().equals(userId)) {
      throw new CustomException(GlobalErrorCode.FORBIDDEN);
    }

    subscriptionRepository.delete(subscription);
    log.info("키워드 구독 취소 성공 - userId: {}, keyword: {}", userId, subscription.getKeyword());
  }

  public List<SubscriptionResponse> getSubscriptions(String userId) {
    log.info("구독 목록 조회 요청 - userId: {}", userId);
    return subscriptionRepository.findByUserId(userId).stream()
        .map(SubscriptionResponse::from)
        .toList();
  }

  public List<NotificationResponse> getNotifications(String userId) {
    log.info("알림 목록 조회 요청 - userId: {}", userId);
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(NotificationResponse::from)
        .toList();
  }

  @Transactional
  public void markAsRead(String userId, Long notificationId) {
    log.info("알림 읽음 처리 요청 - userId: {}, notificationId: {}", userId, notificationId);

    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND));

    if (!notification.getUserId().equals(userId)) {
      throw new CustomException(GlobalErrorCode.FORBIDDEN);
    }

    notification.markAsRead();
    log.info("알림 읽음 처리 성공 - notificationId: {}", notificationId);
  }

  // 트렌딩 갱신 시 구독 키워드와 매칭해서 알림 생성
  @Transactional
  public void createNotificationsFromSnapshots(List<TrendingSnapshot> snapshots) {
    List<Subscription> subscriptions = subscriptionRepository.findAll();
    if (subscriptions.isEmpty()) return;

    List<Notification> notifications =
        snapshots.stream()
            .flatMap(
                snapshot ->
                    subscriptions.stream()
                        .filter(
                            sub ->
                                snapshot
                                    .getContent()
                                    .getTitle()
                                    .toLowerCase()
                                    .contains(sub.getKeyword().toLowerCase()))
                        .map(
                            sub ->
                                Notification.builder()
                                    .userId(sub.getUserId())
                                    .keyword(sub.getKeyword())
                                    .contentTitle(snapshot.getContent().getTitle())
                                    .contentUrl(snapshot.getContent().getUrl())
                                    .isRead(false)
                                    .createdAt(LocalDateTime.now())
                                    .build()))
            .toList();

    if (!notifications.isEmpty()) {
      notificationRepository.saveAll(notifications);
      log.info("구독 알림 생성 완료 - {}건", notifications.size());
    }
  }
}
