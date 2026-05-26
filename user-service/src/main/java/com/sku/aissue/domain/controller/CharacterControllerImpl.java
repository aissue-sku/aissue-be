/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.request.CharacterItemRequest;
import com.sku.aissue.domain.dto.response.CharacterResponse;
import com.sku.aissue.domain.entity.ItemType;
import com.sku.aissue.domain.service.CharacterService;
import com.sku.aissue.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CharacterControllerImpl implements CharacterController {

  private final CharacterService characterService;

  @Override
  public ResponseEntity<BaseResponse<CharacterResponse>> getCharacter() {
    return ResponseEntity.ok(BaseResponse.success(characterService.getCharacter()));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> purchaseItem(CharacterItemRequest request) {
    characterService.purchaseItem(request);
    return ResponseEntity.ok(BaseResponse.success());
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> equipItem(CharacterItemRequest request) {
    characterService.equipItem(request);
    return ResponseEntity.ok(BaseResponse.success());
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> unequipItem(ItemType itemType) {
    characterService.unequipItem(itemType);
    return ResponseEntity.ok(BaseResponse.success());
  }
}
