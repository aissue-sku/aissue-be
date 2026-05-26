/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_character")
public class UserCharacter {

  /** users.id와 1:1 매핑 — 별도 FK 없이 id로 관리 */
  @Id
  @Column(name = "user_id")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private CharacterItem hat;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  @Builder.Default
  private CharacterItem face = CharacterItem.BASIC;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private CharacterItem clothes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  @Builder.Default
  private CharacterItem color = CharacterItem.AISSUE;

  public void equipItem(CharacterItem item) {
    switch (item.getType()) {
      case HAT -> this.hat = item;
      case FACE -> this.face = item;
      case CLOTHES -> this.clothes = item;
      case COLOR -> this.color = item;
    }
  }

  public void removeHat() {
    this.hat = null;
  }

  public void removeClothes() {
    this.clothes = null;
  }
}
