/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sku.aissue.domain.dto.request.CharacterItemRequest;
import com.sku.aissue.domain.dto.response.CharacterResponse;
import com.sku.aissue.domain.dto.response.EquippedItemResponse;
import com.sku.aissue.domain.dto.response.ItemInfoResponse;
import com.sku.aissue.domain.entity.CharacterItem;
import com.sku.aissue.domain.entity.ItemType;
import com.sku.aissue.domain.entity.User;
import com.sku.aissue.domain.entity.UserCharacter;
import com.sku.aissue.domain.entity.UserItem;
import com.sku.aissue.domain.exception.UserErrorCode;
import com.sku.aissue.domain.repository.UserCharacterRepository;
import com.sku.aissue.domain.repository.UserItemRepository;
import com.sku.aissue.domain.repository.UserRepository;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.global.client.NotificationServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterServiceImpl implements CharacterService {

  private final UserRepository userRepository;
  private final UserCharacterRepository userCharacterRepository;
  private final UserItemRepository userItemRepository;
  private final NotificationServiceClient notificationServiceClient;

  @Override
  public CharacterResponse getCharacter() {
    User user = getCurrentUser();
    UserCharacter character = getOrCreateCharacter(user.getId());

    Set<CharacterItem> ownedItems = getOwnedItems(user.getId());

    List<ItemInfoResponse> allItems =
        Arrays.stream(CharacterItem.values())
            .map(item -> ItemInfoResponse.of(item, ownedItems.contains(item)))
            .toList();

    CharacterResponse.EquippedResponse equipped =
        CharacterResponse.EquippedResponse.builder()
            .hat(EquippedItemResponse.from(character.getHat()))
            .face(EquippedItemResponse.from(character.getFace()))
            .clothes(EquippedItemResponse.from(character.getClothes()))
            .color(EquippedItemResponse.from(character.getColor()))
            .build();

    return CharacterResponse.builder().equipped(equipped).items(allItems).build();
  }

  @Override
  @Transactional
  public void purchaseItem(CharacterItemRequest request) {
    User user = getCurrentUser();
    CharacterItem item = resolveItem(request.getItem());

    if (item.isDefaultOwned()) {
      throw new CustomException(UserErrorCode.ITEM_DEFAULT_OWNED);
    }

    if (userItemRepository.existsByUserIdAndItem(user.getId(), item)) {
      throw new CustomException(UserErrorCode.ITEM_ALREADY_PURCHASED);
    }

    if (user.getPoints() < item.getPrice()) {
      throw new CustomException(UserErrorCode.INSUFFICIENT_POINTS);
    }

    user.deductPoints(item.getPrice());
    userRepository.save(user);

    userItemRepository.save(
        UserItem.builder()
            .userId(user.getId())
            .item(item)
            .purchasedAt(LocalDateTime.now())
            .build());

    log.info(
        "아이템 구매 완료 - userId: {}, item: {}, 차감: {}p, 잔여: {}p",
        user.getId(),
        item,
        item.getPrice(),
        user.getPoints());

    notificationServiceClient.sendDirect(
        user.getUsername(),
        "포인트",
        String.format(
            "'%s' 구매 완료! -%d포인트 (잔여: %d포인트)",
            item.getDisplayName(), item.getPrice(), user.getPoints()));
  }

  @Override
  @Transactional
  public void equipItem(CharacterItemRequest request) {
    User user = getCurrentUser();
    CharacterItem item = resolveItem(request.getItem());

    if (!isOwned(user.getId(), item)) {
      throw new CustomException(UserErrorCode.ITEM_NOT_PURCHASED);
    }

    UserCharacter character = getOrCreateCharacter(user.getId());
    character.equipItem(item);
    userCharacterRepository.save(character);

    log.info("아이템 착용 완료 - userId: {}, item: {}", user.getId(), item);
  }

  @Override
  @Transactional
  public void unequipItem(ItemType itemType) {
    if (itemType == ItemType.FACE || itemType == ItemType.COLOR) {
      throw new CustomException(UserErrorCode.ITEM_UNEQUIP_NOT_ALLOWED);
    }

    User user = getCurrentUser();
    UserCharacter character = getOrCreateCharacter(user.getId());

    if (itemType == ItemType.HAT) {
      character.removeHat();
    } else {
      character.removeClothes();
    }

    userCharacterRepository.save(character);
    log.info("아이템 해제 완료 - userId: {}, type: {}", user.getId(), itemType);
  }

  private CharacterItem resolveItem(String key) {
    CharacterItem item = CharacterItem.fromKey(key);
    if (item == null) {
      throw new CustomException(UserErrorCode.ITEM_NOT_FOUND);
    }
    return item;
  }

  private boolean isOwned(Long userId, CharacterItem item) {
    if (item.isDefaultOwned()) return true;
    return userItemRepository.existsByUserIdAndItem(userId, item);
  }

  private Set<CharacterItem> getOwnedItems(Long userId) {
    Set<CharacterItem> purchased =
        userItemRepository.findByUserId(userId).stream()
            .map(UserItem::getItem)
            .collect(Collectors.toSet());

    // 기본 제공 아이템 추가
    Arrays.stream(CharacterItem.values())
        .filter(CharacterItem::isDefaultOwned)
        .forEach(purchased::add);

    return purchased;
  }

  /** 캐릭터 정보가 없으면 기본값으로 생성 (신규 유저 첫 조회 시) */
  private UserCharacter getOrCreateCharacter(Long userId) {
    return userCharacterRepository
        .findByUserId(userId)
        .orElseGet(
            () -> userCharacterRepository.save(UserCharacter.builder().userId(userId).build()));
  }

  private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new CustomException(UserErrorCode.UNAUTHORIZED);
    }

    String username = (String) authentication.getPrincipal();

    if (username == null || username.isBlank()) {
      throw new CustomException(UserErrorCode.UNAUTHORIZED);
    }

    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
  }
}
