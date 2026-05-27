/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sku.aissue.domain.dto.request.ContentSubmitRequest;
import com.sku.aissue.domain.dto.response.AnalysisHistoryResponse;
import com.sku.aissue.domain.dto.response.AnalysisScoreResponse;
import com.sku.aissue.domain.dto.response.ArticleCritiqueResponse;
import com.sku.aissue.domain.dto.response.ContentResponse;
import com.sku.aissue.domain.dto.response.HotTopicResponse;
import com.sku.aissue.domain.dto.response.NewsItemResponse;
import com.sku.aissue.domain.dto.response.TrendingContentResponse;
import com.sku.aissue.domain.dto.response.TrendingKeywordResponse;
import com.sku.aissue.domain.entity.PeriodType;
import com.sku.aissue.global.page.InfiniteResponse;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "콘텐츠", description = "콘텐츠 수집 및 조회 API")
@RequestMapping("/api/contents")
public interface ContentController {

  @GetMapping("/trending")
  @SecurityRequirements
  @Operation(
      summary = "트렌딩 콘텐츠 조회",
      description =
          """
          현재 트렌딩 이슈/뉴스 목록을 순위 순으로 반환합니다. **인증 불필요.**
          - `HOURLY`: 최근 1시간 내 수집된 콘텐츠 기준 (매 시간 갱신)
          - `DAILY`: 최근 24시간 내 수집된 콘텐츠 기준 (매일 자정 갱신)
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": [
                                {
                                  "rankOrder": 1,
                                  "content": {
                                    "id": 42,
                                    "title": "트럼프, 관세 협상 타결 임박…한국도 포함",
                                    "body": "미국 트럼프 대통령이 주요 교역국과의 관세 협상이 마무리 단계에 접어들었다고 밝혔다.",
                                    "url": "https://www.hani.co.kr/arti/economy/economy_general/1234567.html",
                                    "source": "한겨레",
                                    "category": null,
                                    "contentType": "NEWS",
                                    "contentSource": "RSS",
                                    "publishedAt": "2026-04-28T10:30:00",
                                    "collectedAt": "2026-04-28T10:35:12"
                                  }
                                },
                                {
                                  "rankOrder": 2,
                                  "content": {
                                    "id": 43,
                                    "title": "반도체 수출 규제 완화 논의…삼성·SK 기대감",
                                    "body": "미국 상무부가 한국 반도체 기업에 대한 수출 규제를 일부 완화하는 방안을 검토 중이다.",
                                    "url": "https://n.news.naver.com/article/001/0013456789",
                                    "source": "연합뉴스",
                                    "category": null,
                                    "contentType": "NEWS",
                                    "contentSource": "NAVER_NEWS",
                                    "publishedAt": "2026-04-28T09:15:00",
                                    "collectedAt": "2026-04-28T09:20:05"
                                  }
                                }
                              ]
                            }
                            """)))
  })
  ResponseEntity<BaseResponse<List<TrendingContentResponse>>> getTrending(
      @Parameter(description = "집계 주기 (HOURLY / DAILY)", example = "HOURLY")
          @RequestParam(defaultValue = "HOURLY")
          PeriodType periodType);

  @GetMapping("/{id}/critique")
  @SecurityRequirements
  @Operation(
      summary = "기사 비평 조회 (2페이지)",
      description = "스케줄러가 사전 계산한 비평 분석 결과를 반환합니다. 아직 분석되지 않은 경우 404를 반환합니다. **인증 불필요.**")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(
        responseCode = "404",
        description = "분석 결과 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "code": 404,
                              "message": "아직 분석되지 않은 콘텐츠입니다.",
                              "data": null
                            }
                            """)))
  })
  ResponseEntity<BaseResponse<ArticleCritiqueResponse>> getCritiqueByContentId(
      @Parameter(description = "콘텐츠 ID", example = "42") @PathVariable Long id);

  @GetMapping("/{id}/analysis")
  @SecurityRequirements
  @Operation(
      summary = "기사 신뢰도 분석 조회 (3페이지)",
      description = "스케줄러가 사전 계산한 신뢰도 점수 분석 결과를 반환합니다. 아직 분석되지 않은 경우 404를 반환합니다. **인증 불필요.**")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(
        responseCode = "404",
        description = "분석 결과 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "code": 404,
                              "message": "아직 분석되지 않은 콘텐츠입니다.",
                              "data": null
                            }
                            """)))
  })
  ResponseEntity<BaseResponse<AnalysisScoreResponse>> getAnalysisByContentId(
      @Parameter(description = "콘텐츠 ID", example = "42") @PathVariable Long id);

  @GetMapping("/{id}")
  @SecurityRequirements
  @Operation(summary = "콘텐츠 상세 조회", description = "콘텐츠 ID로 상세 정보를 조회합니다. **인증 불필요.**")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "id": 42,
                                "title": "트럼프, 관세 협상 타결 임박…한국도 포함",
                                "body": "미국 트럼프 대통령이 주요 교역국과의 관세 협상이 마무리 단계에 접어들었다고 밝혔다. 한국을 포함한 주요 동맹국과의 협상이 이번 주 내 타결될 가능성이 높다.",
                                "url": "https://www.hani.co.kr/arti/economy/economy_general/1234567.html",
                                "source": "한겨레",
                                "category": null,
                                "contentType": "NEWS",
                                "contentSource": "RSS",
                                "publishedAt": "2026-04-28T10:30:00",
                                "collectedAt": "2026-04-28T10:35:12"
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "콘텐츠 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "code": 404,
                              "message": "요청한 리소스를 찾을 수 없습니다.",
                              "data": null
                            }
                            """)))
  })
  ResponseEntity<BaseResponse<ContentResponse>> getById(
      @Parameter(description = "콘텐츠 ID", example = "42") @PathVariable Long id);

  @GetMapping("/search")
  @SecurityRequirements
  @Operation(summary = "콘텐츠 검색", description = "제목 기준 키워드 검색 (대소문자 무시). **인증 불필요.**")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "검색 성공 (결과 없을 시 빈 배열 반환)",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "검색 결과 있음",
                      value =
                          """
                          {
                            "success": true,
                            "code": 200,
                            "message": "요청이 성공적으로 처리되었습니다.",
                            "data": [
                              {
                                "id": 42,
                                "title": "트럼프, 관세 협상 타결 임박…한국도 포함",
                                "body": "미국 트럼프 대통령이 주요 교역국과의 관세 협상이 마무리 단계에 접어들었다고 밝혔다.",
                                "url": "https://www.hani.co.kr/arti/economy/economy_general/1234567.html",
                                "source": "한겨레",
                                "category": null,
                                "contentType": "NEWS",
                                "contentSource": "RSS",
                                "publishedAt": "2026-04-28T10:30:00",
                                "collectedAt": "2026-04-28T10:35:12"
                              }
                            ]
                          }
                          """),
                  @ExampleObject(
                      name = "검색 결과 없음",
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
  ResponseEntity<BaseResponse<List<ContentResponse>>> search(
      @Parameter(description = "검색 키워드", example = "트럼프") @RequestParam String keyword);

  @PostMapping("/trending/refresh")
  @SecurityRequirements
  @Operation(summary = "트렌딩 수동 갱신", description = "스케줄러를 기다리지 않고 트렌딩 스냅샷을 즉시 갱신합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "갱신 성공")})
  ResponseEntity<BaseResponse<Void>> refreshTrending();

  @PostMapping("/issue-cards/refresh")
  @SecurityRequirements
  @Operation(
      summary = "이슈 카드 수동 생성",
      description = "스케줄러를 기다리지 않고 AI 이슈 카드를 즉시 생성하여 DB에 저장합니다. (HOURLY/DAILY 모두 생성)")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "생성 성공")})
  ResponseEntity<BaseResponse<Void>> refreshIssueCards();

  @PostMapping("/hot-topics/refresh")
  @SecurityRequirements
  @Operation(summary = "급상승 키워드 수동 갱신", description = "스케줄러를 기다리지 않고 급상승 키워드를 즉시 분석하여 DB에 저장합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "갱신 성공")})
  ResponseEntity<BaseResponse<List<HotTopicResponse>>> refreshHotTopics();

  @PostMapping("/submit")
  @Operation(
      summary = "콘텐츠 제출 (신뢰도 분석 요청)",
      description =
          """
          URL 또는 텍스트를 제출하여 신뢰도 분석을 요청합니다. **로그인 필요.**
          - `URL`: 기사 URL을 제출하면 본문을 자동 추출합니다.
          - `TEXT`: 직접 텍스트를 입력합니다. (최대 50자가 제목으로 저장)
          - 분석은 비동기로 처리되며, 수락 시 202를 반환합니다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "분석 완료",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "totalScore": 72,
                                "credibility": 18,
                                "accuracy": 20,
                                "bias": 14,
                                "crossVerification": 14,
                                "transparency": 6,
                                "verdict": "신뢰할 수 있음",
                                "reason": "공신력 있는 언론사를 출처로 인용하고 있으며, 주요 사실 관계가 확인 가능합니다. 다만 일부 주관적 표현이 포함되어 있어 편향성 점수가 다소 낮습니다."
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "입력값 누락",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "code": 400,
                              "message": "content 값은 필수입니다.",
                              "data": null
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패 (토큰 없음 또는 만료)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "code": 401,
                              "message": "인증 정보가 없습니다.",
                              "data": null
                            }
                            """)))
  })
  ResponseEntity<BaseResponse<AnalysisScoreResponse>> submit(
      @RequestBody @Valid ContentSubmitRequest request, @AuthenticationPrincipal String username);

  @GetMapping("/analysis/history")
  @Operation(summary = "신뢰도 분석 이력 조회", description = "내가 분석 요청한 콘텐츠 목록과 점수를 반환합니다. **로그인 필요.**")
  ResponseEntity<BaseResponse<List<AnalysisHistoryResponse>>> getAnalysisHistory(
      @AuthenticationPrincipal String username);

  @GetMapping("/keyword-news")
  @SecurityRequirements
  @Operation(
      summary = "키워드 관련 뉴스 조회 (무한스크롤)",
      description =
          """
          키워드가 제목에 포함된 뉴스를 최신순으로 반환합니다. **인증 불필요.**
          - 첫 요청: `cursor` 생략
          - 다음 요청: 이전 응답의 `lastCursor` 값을 `cursor`로 전달
          - `hasNext: false`이면 마지막 페이지입니다.
          """)
  @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
  ResponseEntity<BaseResponse<InfiniteResponse<NewsItemResponse>>> getKeywordNews(
      @Parameter(description = "검색 키워드", example = "전기차 화재") @RequestParam String keyword,
      @Parameter(description = "커서 (이전 응답의 lastCursor, 첫 요청 시 생략)") @RequestParam(required = false)
          Long cursor,
      @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10")
          int size);

  @PostMapping("/critique")
  @Operation(
      summary = "기사 비평 분석",
      description =
          """
          기사의 신뢰 근거와 글쓰기 패턴을 분석합니다. **로그인 필요.**
          - `URL`: 기사 URL을 제출하면 본문을 자동 추출합니다.
          - `TEXT`: 직접 텍스트를 입력합니다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "분석 완료",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "trustReasons": ["공신력 있는 출처 인용", "구체적 수치 제시"],
                                "critiques": [
                                  {
                                    "label": "불확실 표현",
                                    "count": 3,
                                    "status": "확인",
                                    "description": "→ '인 것 같다', '보인다' 등 불확실 표현이 반복됩니다."
                                  }
                                ]
                              }
                            }
                            """)))
  })
  ResponseEntity<BaseResponse<ArticleCritiqueResponse>> critique(
      @RequestBody @Valid ContentSubmitRequest request, @AuthenticationPrincipal String username);

  @GetMapping("/hot-topics")
  @SecurityRequirements
  @Operation(
      summary = "급상승 키워드 조회",
      description =
          """
          최근 1시간 수집량이 평소 대비 급증한 키워드 목록을 반환합니다. **인증 불필요.**
          - 로그인 상태이면 `subscribed` 필드에 구독 여부가 반영됩니다.
          - 비로그인 시 `subscribed`는 항상 `false`입니다.
          """)
  ResponseEntity<BaseResponse<List<TrendingKeywordResponse>>> getHotTopics(
      @AuthenticationPrincipal String username);
}
