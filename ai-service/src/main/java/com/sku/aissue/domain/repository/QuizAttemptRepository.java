/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sku.aissue.domain.entity.QuizAttempt;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

  Optional<QuizAttempt> findByUsernameAndQuizDate(String username, LocalDate quizDate);

  boolean existsByUsernameAndQuizDate(String username, LocalDate quizDate);

  void deleteByQuizDate(LocalDate quizDate);
}
