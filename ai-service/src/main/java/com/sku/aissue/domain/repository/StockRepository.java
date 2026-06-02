/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sku.aissue.domain.entity.Stock;

public interface StockRepository extends JpaRepository<Stock, String> {

  // 뉴스 제목에서 종목명 추출: 제목에 포함된 종목 조회 (길이 긴 순 → 짧은 종목명에 의한 오매칭 방지)
  @Query(
      "SELECT s FROM Stock s WHERE :title LIKE CONCAT('%', s.name, '%') ORDER BY LENGTH(s.name) DESC")
  List<Stock> findByNameContainedIn(@Param("title") String title);

  // 최근 가격 조회된 상위 종목 (폴백용)
  List<Stock> findTop3ByLastPriceUpdatedIsNotNullOrderByLastPriceUpdatedDesc();
}
