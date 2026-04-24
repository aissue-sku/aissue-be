/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sku.aissue.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);
}
