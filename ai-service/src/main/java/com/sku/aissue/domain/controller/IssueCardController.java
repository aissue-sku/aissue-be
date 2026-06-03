/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.response.IssueCardResponse;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "이슈 카드", description = "트렌딩 이슈 카드 분석 API")
@RequestMapping("/api/issues")
public interface IssueCardController {

  @GetMapping("/cards")
  @SecurityRequirements
  @Operation(
      summary = "이슈 카드 목록 조회",
      description =
          """
          급상승 키워드 기반으로 AI가 생성한 이슈 카드 목록을 반환합니다. **인증 불필요.**
          - 00시 / 06시 / 12시 / 18시 기준으로 갱신됩니다.
          - 최대 10개 카드를 반환합니다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "카드 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "정상 응답",
                      value =
                          """
                          {
                            "success": true,
                            "code": 200,
                            "message": "요청이 성공적으로 처리되었습니다.",
                            "data": [
                              {
                                "id": "42",
                                "title": "한국인이 가장 분노한 그 발언, 실제로 무슨 말이었나?",
                                "teaser": "진짜 피해자는 따로 있다고?",
                                "imageUrl": null,
                                "timeAgo": "32분 전",
                                "tags": ["트럼프 관세", "무역전쟁", "미국"],
                                "category": "경제",
                                "url": "https://www.hani.co.kr/arti/economy/1234567.html"
                              }
                            ]
                          }
                          """)
                }))
  })
  ResponseEntity<BaseResponse<List<IssueCardResponse>>> getIssueCards(
      @AuthenticationPrincipal String username);

  @PostMapping("/cards/refresh")
  @SecurityRequirements
  @Operation(
      summary = "이슈 카드 수동 생성 (이미지 포함)",
      description = "스케줄러를 기다리지 않고 AI 이슈 카드를 즉시 생성합니다. 각 카드의 이미지를 OpenAI로 생성하여 S3에 업로드합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "생성 성공")})
  ResponseEntity<BaseResponse<Void>> refreshIssueCards();

  @PostMapping("/cards/refresh/no-image")
  @SecurityRequirements
  @Operation(
      summary = "이슈 카드 수동 생성 (이미지 제외)",
      description =
          "스케줄러를 기다리지 않고 AI 이슈 카드를 즉시 생성합니다. 이미지 생성을 건너뛰어 호출 비용과 시간을 줄입니다. imageUrl은 null로 저장됩니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "생성 성공")})
  ResponseEntity<BaseResponse<Void>> refreshIssueCardsWithoutImage();
}
