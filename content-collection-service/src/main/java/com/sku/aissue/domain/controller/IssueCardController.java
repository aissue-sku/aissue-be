/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
                                "imageUrl": "https://aissue-bucket.s3.ap-northeast-2.amazonaws.com/issue-cards/uuid.png",
                                "timeAgo": "32분 전",
                                "tags": ["트럼프 관세", "무역전쟁", "미국"],
                                "category": "경제",
                                "url": "https://www.hani.co.kr/arti/economy/1234567.html"
                              }
                            ]
                          }
                          """),
                  @ExampleObject(
                      name = "데이터 없음",
                      value =
                          """
                          {
                            "success": true,
                            "code": 200,
                            "message": "요청이 성공적으로 처리되었습니다.",
                            "data": []
                          }
                          """)
                }))
  })
  ResponseEntity<BaseResponse<List<IssueCardResponse>>> getIssueCards();
}
