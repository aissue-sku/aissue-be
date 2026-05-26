/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sku.aissue.domain.entity.PeriodType;
import com.sku.aissue.domain.entity.TrendingSnapshot;

public interface TrendingSnapshotRepository extends JpaRepository<TrendingSnapshot, Long> {

  // 최신 트렌딩 스냅샷 목록 조회 (rank 오름차순)
  @Query(
      """
      SELECT t FROM TrendingSnapshot t
      JOIN FETCH t.content
      WHERE t.periodType = :periodType
        AND t.snapshotAt = (
          SELECT MAX(t2.snapshotAt) FROM TrendingSnapshot t2
          WHERE t2.periodType = :periodType
        )
      ORDER BY t.rankOrder ASC
      """)
  List<TrendingSnapshot> findLatestByPeriodType(@Param("periodType") PeriodType periodType);

  // 오래된 스냅샷 삭제용 (보관 기간 초과분 정리)
  void deleteBySnapshotAtBefore(LocalDateTime before);
}
