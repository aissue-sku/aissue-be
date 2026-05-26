/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.entity;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CharacterItem {

  // 모자 (HAT) - optional
  MANGO_HAT(ItemType.HAT, "망고 모자", false, 200),
  BERET(ItemType.HAT, "베레모", false, 150),
  BEAR_HOOD(ItemType.HAT, "곰 후드", false, 300),
  GENTLEMEN_HAT(ItemType.HAT, "신사 모자", false, 250),
  BASEBALL_CAP(ItemType.HAT, "야구 모자", false, 150),
  HEADPHONE(ItemType.HAT, "헤드폰", false, 300),

  // 얼굴 (FACE) - required, BASIC is free default
  BASIC(ItemType.FACE, "기본", true, 0),
  HAPPY(ItemType.FACE, "행복", false, 100),
  MISCHIEF(ItemType.FACE, "장난", false, 100),
  SAD(ItemType.FACE, "슬픔", false, 100),
  ANGRY(ItemType.FACE, "화남", false, 150),
  SENSITIVE(ItemType.FACE, "예민", false, 150),
  TIRED(ItemType.FACE, "피곤", false, 100),

  // 옷 (CLOTHES) - optional
  BROWN_SHIRT(ItemType.CLOTHES, "브라운 셔츠", false, 200),
  GREEN_SHIRT(ItemType.CLOTHES, "그린 셔츠", false, 200),
  PAJAMA(ItemType.CLOTHES, "파자마", false, 250),
  PANTS(ItemType.CLOTHES, "바지", false, 150),
  PINK_SKIRT(ItemType.CLOTHES, "핑크 치마", false, 200),
  SUIT(ItemType.CLOTHES, "정장", false, 350),

  // 색상 (COLOR) - required, AISSUE is free default
  AISSUE(ItemType.COLOR, "아이슈", true, 0),
  JANMANG(ItemType.COLOR, "잔망슈", false, 200),
  SUNSET(ItemType.COLOR, "선셋슈", false, 200),
  RETRO(ItemType.COLOR, "레트로슈", false, 250),
  SWEET(ItemType.COLOR, "달콤슈", false, 200),
  COOL(ItemType.COLOR, "쿨슈", false, 250);

  private final ItemType type;
  private final String displayName;

  /** 기본 제공 아이템 — 구매 없이 누구나 소유 */
  private final boolean defaultOwned;

  private final int price;

  public static List<CharacterItem> getByType(ItemType type) {
    return Arrays.stream(values()).filter(item -> item.type == type).toList();
  }

  public static CharacterItem fromKey(String key) {
    try {
      return CharacterItem.valueOf(key.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
