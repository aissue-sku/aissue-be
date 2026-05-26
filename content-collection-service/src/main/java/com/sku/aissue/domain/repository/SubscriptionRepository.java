/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sku.aissue.domain.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

  List<Subscription> findByUserId(String userId);

  List<Subscription> findAll();

  Optional<Subscription> findByUserIdAndKeyword(String userId, String keyword);

  boolean existsByUserIdAndKeyword(String userId, String keyword);
}
