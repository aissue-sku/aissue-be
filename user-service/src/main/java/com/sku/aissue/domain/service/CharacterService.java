/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import com.sku.aissue.domain.dto.request.CharacterItemRequest;
import com.sku.aissue.domain.dto.response.CharacterResponse;
import com.sku.aissue.domain.entity.ItemType;

public interface CharacterService {

  CharacterResponse getCharacter();

  void purchaseItem(CharacterItemRequest request);

  void equipItem(CharacterItemRequest request);

  void unequipItem(ItemType itemType);
}
