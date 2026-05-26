/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sku.aissue.domain.entity.TrendingKeyword;

public interface TrendingKeywordRepository extends JpaRepository<TrendingKeyword, Long> {

  @Query(
      """
      SELECT t FROM TrendingKeyword t
      WHERE t.snapshotAt = (SELECT MAX(t2.snapshotAt) FROM TrendingKeyword t2)
      ORDER BY t.count DESC
      """)
  List<TrendingKeyword> findLatest();

  void deleteBySnapshotAtBefore(LocalDateTime before);
}
