/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sku.aissue.domain.entity.StockPrice;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

  @Query("SELECT p FROM StockPrice p WHERE p.stockCode = :code ORDER BY p.tradeDate DESC")
  List<StockPrice> findByStockCodeOrderByTradeDateDesc(@Param("code") String code);

  @Modifying
  @Query("DELETE FROM StockPrice p WHERE p.stockCode = :code")
  void deleteByStockCode(@Param("code") String code);
}
