/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.request.LoginRequest;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "인증", description = "인증 관련 API")
@RequestMapping("/api/auths")
public interface AuthController {

  @Operation(
      summary = "로그인",
      description =
          """
          아이디/비밀번호로 로그인합니다.
          - **Access Token**: `ACCESS_TOKEN` HttpOnly 쿠키로 전달
          - **Refresh Token**: `REFRESH_TOKEN` HttpOnly 쿠키로 전달
          - 응답 body의 `data`는 로그인한 사용자의 아이디입니다.
          """)
  @SecurityRequirements
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "로그인 성공",
        headers = {
          @Header(
              name = "Authorization",
              description = "Bearer {accessToken}",
              schema = @Schema(type = "string"))
        },
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BaseResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "로그인이 완료되었습니다.",
                              "data": "aissue"
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "입력값 유효성 오류 (아이디/비밀번호 형식 불일치)",
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
                              "message": "로그인 아이디는 4~20자여야 합니다.",
                              "data": null
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "아이디 또는 비밀번호 불일치",
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
                              "message": "아이디 또는 비밀번호가 일치하지 않습니다.",
                              "data": null
                            }
                            """)))
  })
  @PostMapping("/login")
  ResponseEntity<BaseResponse<String>> login(
      HttpServletResponse response,
      @Parameter(description = "로그인 정보") @RequestBody @Valid LoginRequest loginRequest);

  @Operation(
      summary = "로그아웃",
      description =
          """
          로그아웃을 수행합니다.
          - Redis에 저장된 Refresh Token을 삭제합니다.
          - Access Token을 블랙리스트에 등록합니다.
          - `REFRESH_TOKEN` 쿠키를 만료 처리합니다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "로그아웃 성공",
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
                              "message": "로그아웃이 완료되었습니다.",
                              "data": "로그아웃 성공 - 사용자: aissue"
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "Access Token 없음 또는 만료",
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
                              "message": "액세스 토큰이 없습니다.",
                              "data": null
                            }
                            """)))
  })
  @Parameters({
    @Parameter(
        name = "Authorization",
        in = ParameterIn.HEADER,
        description = "Bearer {accessToken}",
        required = true,
        example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."),
    @Parameter(
        name = "REFRESH_TOKEN",
        in = ParameterIn.COOKIE,
        description = "Refresh Token (HttpOnly 쿠키)")
  })
  @PostMapping("/logout")
  ResponseEntity<BaseResponse<String>> logout(
      HttpServletRequest request, HttpServletResponse response);

  @Operation(
      summary = "Access Token 재발급",
      description =
          """
          Refresh Token을 사용해 새로운 Access Token을 발급합니다.
          - 새 **Access Token**: `ACCESS_TOKEN` HttpOnly 쿠키로 전달
          - 응답 body의 `data`는 `null`입니다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Access Token 재발급 성공",
        headers = {
          @Header(
              name = "Authorization",
              description = "Bearer {newAccessToken}",
              schema = @Schema(type = "string"))
        },
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
                              "message": "액세스 토큰 재발급이 완료되었습니다.",
                              "data": null
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "Refresh Token 없음, 만료, 서명 불일치 또는 타입 불일치",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "토큰 없음",
                      value =
                          """
                          {
                            "success": false,
                            "code": 401,
                            "message": "리프레시 토큰이 없습니다.",
                            "data": null
                          }
                          """),
                  @ExampleObject(
                      name = "토큰 만료/불일치",
                      value =
                          """
                          {
                            "success": false,
                            "code": 401,
                            "message": "유효하지 않은 리프레시 토큰입니다.",
                            "data": null
                          }
                          """)
                }))
  })
  @Parameter(
      name = "REFRESH_TOKEN",
      in = ParameterIn.COOKIE,
      description = "Refresh Token (HttpOnly 쿠키)",
      required = true)
  @PostMapping("/refresh")
  ResponseEntity<BaseResponse<String>> reissueAccessToken(
      HttpServletRequest request, HttpServletResponse response);

  @Operation(
      summary = "[테스트] 고정 계정 로그인",
      description =
          """
          **개발/테스트 전용** 고정 계정(aissue / qwer1234!)으로 즉시 로그인합니다.
          - DB 사용자 없이도 동작합니다.
          - Access Token / Refresh Token 을 쿠키로 내려줍니다.
          """)
  @SecurityRequirements
  @PostMapping("/test-login")
  ResponseEntity<BaseResponse<String>> testLogin(HttpServletResponse response);
}
