/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "stock")
public class Stock {

  @Id
  @Column(length = 10)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 10)
  private String market; // KOSPI / KOSDAQ

  @Column(name = "last_price_updated")
  private LocalDateTime lastPriceUpdated;

  public void markPriceUpdated() {
    this.lastPriceUpdated = LocalDateTime.now();
  }
}
