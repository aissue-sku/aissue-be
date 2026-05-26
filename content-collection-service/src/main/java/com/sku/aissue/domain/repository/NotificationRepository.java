/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sku.aissue.domain.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

  long countByUserIdAndIsReadFalse(String userId);
}
