/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sku.aissue.domain.entity.IssueCard;

public interface IssueCardRepository extends JpaRepository<IssueCard, Long> {

  // 공통 카드 (userId IS NULL)
  @Query(
      """
      SELECT i FROM IssueCard i
      WHERE i.userId IS NULL
        AND i.snapshotAt = (SELECT MAX(i2.snapshotAt) FROM IssueCard i2 WHERE i2.userId IS NULL)
      ORDER BY i.rankOrder ASC
      """)
  List<IssueCard> findLatest();

  // 구독 키워드에 해당하는 공통 카드 (read-time 개인화용)
  @Query(
      """
      SELECT i FROM IssueCard i
      WHERE i.userId IS NULL
        AND i.snapshotAt = (SELECT MAX(i2.snapshotAt) FROM IssueCard i2 WHERE i2.userId IS NULL)
        AND i.keyword IN :keywords
      ORDER BY i.rankOrder ASC
      """)
  List<IssueCard> findLatestByKeywords(@Param("keywords") List<String> keywords);

  void deleteBySnapshotAtBefore(LocalDateTime before);
}
