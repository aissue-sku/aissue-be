/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sku.aissue.domain.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

  List<Subscription> findByUserId(String userId);

  Optional<Subscription> findByUserIdAndKeyword(String userId, String keyword);

  boolean existsByUserIdAndKeyword(String userId, String keyword);

  List<Subscription> findByKeyword(String keyword);

  void deleteByUserIdAndKeyword(String userId, String keyword);

  @Query(
      "SELECT s.keyword, COUNT(s.userId) FROM Subscription s GROUP BY s.keyword ORDER BY COUNT(s.userId) DESC")
  List<Object[]> findTopKeywordsBySubscriberCount(Pageable pageable);
}
