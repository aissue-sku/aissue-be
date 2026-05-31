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

  @Query(
      """
      SELECT i FROM IssueCard i
      WHERE i.snapshotAt = (SELECT MAX(i2.snapshotAt) FROM IssueCard i2)
      ORDER BY i.rankOrder ASC
      """)
  List<IssueCard> findLatest();

  @Query(
      """
      SELECT i FROM IssueCard i
      WHERE i.snapshotAt = (SELECT MAX(i2.snapshotAt) FROM IssueCard i2)
        AND i.keyword IN :keywords
      ORDER BY i.rankOrder ASC
      """)
  List<IssueCard> findLatestByKeywords(@Param("keywords") List<String> keywords);

  void deleteBySnapshotAtBefore(LocalDateTime before);
}
