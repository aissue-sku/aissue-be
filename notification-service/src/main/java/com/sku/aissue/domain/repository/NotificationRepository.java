/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sku.aissue.domain.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

  long countByUserIdAndIsReadFalse(String userId);

  @Modifying
  @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
  void markAllAsRead(@Param("userId") String userId);

  @Modifying
  @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.isRead = true")
  void deleteReadByUserId(@Param("userId") String userId);

  boolean existsByUserIdAndKeywordAndUrl(String userId, String keyword, String url);
}
