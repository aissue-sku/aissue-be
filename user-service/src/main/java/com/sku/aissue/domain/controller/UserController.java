/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sku.aissue.domain.dto.request.SignUpRequest;
import com.sku.aissue.domain.dto.response.UserDetailResponse;
import com.sku.aissue.domain.dto.response.UserResponse;
import com.sku.aissue.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "사용자", description = "사용자 관련 API")
@RequestMapping("/api/users")
public interface UserController {

  @PostMapping("sign-up")
  @SecurityRequirements
  @Operation(
      summary = "회원가입",
      description =
          """
          신규 사용자를 등록합니다.
          - 아이디: 영문·숫자 4~20자
          - 비밀번호: 영문·숫자·특수문자 포함 8~20자
          - 이름: 한글 또는 영문 2~20자
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "회원가입 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 201,
                              "message": "회원가입이 완료되었습니다.",
                              "data": {
                                "userId": 1,
                                "username": "aissue",
                                "name": "아이슈"
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "입력값 유효성 오류",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "아이디 형식 오류",
                      value =
                          """
                          {
                            "success": false,
                            "code": 400,
                            "message": "아이디는 영문과 숫자만 사용할 수 있습니다.",
                            "data": null
                          }
                          """),
                  @ExampleObject(
                      name = "비밀번호 형식 오류",
                      value =
                          """
                          {
                            "success": false,
                            "code": 400,
                            "message": "비밀번호는 최소 8자 이상, 숫자 및 특수문자를 포함해야 합니다.",
                            "data": null
                          }
                          """)
                })),
    @ApiResponse(
        responseCode = "409",
        description = "이미 존재하는 아이디",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "code": 409,
                              "message": "이미 사용 중인 아이디입니다.",
                              "data": null
                            }
                            """)))
  })
  ResponseEntity<BaseResponse<UserResponse>> signUp(
      @Parameter(description = "회원가입 정보") @RequestBody @Valid SignUpRequest signUpRequest);

  @GetMapping
  @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
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
                                "userId": 1,
                                "username": "aissue",
                                "name": "아이슈"
                              }
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
  ResponseEntity<BaseResponse<UserResponse>> getUserDetail();

  @GetMapping("/dev")
  @Operation(summary = "[개발자] 전체 사용자 조회", description = "전체 사용자 목록을 조회합니다. 개발·관리 목적으로만 사용합니다.")
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
                                  "userId": 1,
                                  "username": "aissue",
                                  "name": "아이슈",
                                  "role": "ROLE_USER"
                                },
                                {
                                  "userId": 2,
                                  "username": "admin",
                                  "name": "관리자",
                                  "role": "ROLE_ADMIN"
                                }
                              ]
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패",
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
  ResponseEntity<BaseResponse<List<UserDetailResponse>>> getAllUsers();

  @DeleteMapping
  @Operation(
      summary = "회원 탈퇴",
      description =
          """
          현재 로그인한 사용자의 계정을 삭제합니다. (Hard Delete)
          - DB에서 사용자 레코드를 완전히 삭제합니다.
          - Redis에 저장된 Refresh Token도 함께 삭제됩니다.
          - `REFRESH_TOKEN` 쿠키를 만료 처리합니다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "탈퇴 성공",
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
                              "data": null
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패",
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
  ResponseEntity<BaseResponse<Void>> deleteUser(HttpServletResponse response);
}
