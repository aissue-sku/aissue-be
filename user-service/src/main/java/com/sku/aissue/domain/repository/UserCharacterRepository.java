/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sku.aissue.domain.entity.UserCharacter;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {

  Optional<UserCharacter> findByUserId(Long userId);
}
