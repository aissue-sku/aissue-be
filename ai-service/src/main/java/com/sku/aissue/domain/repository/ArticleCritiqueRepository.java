/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sku.aissue.domain.entity.ArticleCritique;

public interface ArticleCritiqueRepository extends JpaRepository<ArticleCritique, Long> {

  Optional<ArticleCritique> findTopByUrlOrderByAnalyzedAtDesc(String url);

  Optional<ArticleCritique> findTopByContentIdOrderByAnalyzedAtDesc(Long contentId);

  boolean existsByUrl(String url);
}
