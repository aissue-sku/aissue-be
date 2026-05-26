/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.request.CharacterItemRequest;
import com.sku.aissue.domain.dto.response.CharacterResponse;
import com.sku.aissue.domain.entity.ItemType;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "캐릭터", description = "캐릭터 커스터마이징 API")
@RequestMapping("/api/users/character")
public interface CharacterController {

  @GetMapping
  @Operation(
      summary = "캐릭터 조회",
      description =
          """
          현재 사용자의 캐릭터 착용 정보와 전체 아이템 목록(구매 여부 포함)을 반환합니다.
          - **모자/옷**: 착용 중이지 않으면 null
          - **얼굴/색상**: 항상 값이 존재 (기본값: 기본 / 아이슈)
          - `purchased: true` 인 아이템만 착용 가능
          """)
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "401", description = "인증 실패")
  })
  ResponseEntity<BaseResponse<CharacterResponse>> getCharacter();

  @PostMapping("/purchase")
  @Operation(
      summary = "아이템 구매",
      description =
          """
          아이템을 구매합니다.
          - 기본 제공 아이템(기본 얼굴, 아이슈 색상)은 구매할 수 없습니다.
          - 이미 구매한 아이템은 중복 구매할 수 없습니다.
          """)
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "구매 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "존재하지 않는 아이템 또는 기본 제공 아이템",
        content =
            @Content(
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 400, "message": "기본 제공 아이템은 구매할 수 없습니다.", "data": null }
                            """))),
    @ApiResponse(responseCode = "409", description = "이미 구매한 아이템")
  })
  ResponseEntity<BaseResponse<Void>> purchaseItem(
      @Parameter(description = "구매할 아이템 정보") @RequestBody @Valid CharacterItemRequest request);

  @PutMapping("/equip")
  @Operation(
      summary = "아이템 착용",
      description =
          """
          구매한 아이템을 착용합니다.
          - 아이템 타입에 맞는 슬롯에 자동으로 장착됩니다.
          - 구매하지 않은 아이템은 착용할 수 없습니다.
          """)
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "착용 성공"),
    @ApiResponse(responseCode = "403", description = "구매하지 않은 아이템"),
    @ApiResponse(responseCode = "400", description = "존재하지 않는 아이템")
  })
  ResponseEntity<BaseResponse<Void>> equipItem(
      @Parameter(description = "착용할 아이템 정보") @RequestBody @Valid CharacterItemRequest request);

  @DeleteMapping("/{itemType}")
  @Operation(
      summary = "아이템 해제",
      description =
          """
          착용 중인 아이템을 해제합니다.
          - `HAT`, `CLOTHES` 카테고리만 해제 가능합니다.
          - `FACE`, `COLOR` 는 해제할 수 없습니다 (기본값으로 되돌리려면 기본 아이템을 착용하세요).
          """)
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "해제 성공"),
    @ApiResponse(responseCode = "400", description = "해제 불가능한 카테고리 (FACE, COLOR)")
  })
  ResponseEntity<BaseResponse<Void>> unequipItem(
      @Parameter(description = "해제할 카테고리 (HAT 또는 CLOTHES)", example = "HAT") @PathVariable
          ItemType itemType);
}
