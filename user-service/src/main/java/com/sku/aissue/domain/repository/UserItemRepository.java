/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sku.aissue.domain.entity.CharacterItem;
import com.sku.aissue.domain.entity.UserItem;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

  List<UserItem> findByUserId(Long userId);

  Optional<UserItem> findByUserIdAndItem(Long userId, CharacterItem item);

  boolean existsByUserIdAndItem(Long userId, CharacterItem item);
}
