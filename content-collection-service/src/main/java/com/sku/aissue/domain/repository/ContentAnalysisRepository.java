/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sku.aissue.domain.entity.ContentAnalysis;

public interface ContentAnalysisRepository extends JpaRepository<ContentAnalysis, Long> {

  List<ContentAnalysis> findByUserIdOrderByAnalyzedAtDesc(String userId);

  java.util.Optional<ContentAnalysis> findTopByUrlOrderByAnalyzedAtDesc(String url);
}
