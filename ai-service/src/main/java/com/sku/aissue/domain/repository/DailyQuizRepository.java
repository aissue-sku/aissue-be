/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sku.aissue.domain.entity.DailyQuiz;

public interface DailyQuizRepository extends JpaRepository<DailyQuiz, Long> {

  Optional<DailyQuiz> findByQuizDate(LocalDate quizDate);
}
