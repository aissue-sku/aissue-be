/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "stock_price",
    indexes = {
      @Index(name = "idx_stock_price_code_date", columnList = "stock_code, trade_date DESC")
    })
public class StockPrice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "stock_code", nullable = false, length = 10)
  private String stockCode;

  @Column(name = "trade_date", nullable = false)
  private LocalDate tradeDate;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal open;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal high;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal low;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal close;

  @Column(nullable = false)
  private Long volume;
}
