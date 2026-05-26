/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.controller;

import java.util.List;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.sku.aissue.domain.dto.request.SignUpRequest;
import com.sku.aissue.domain.dto.response.UserDetailResponse;
import com.sku.aissue.domain.dto.response.UserResponse;
import com.sku.aissue.domain.service.UserService;
import com.sku.aissue.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserControllerImpl implements UserController {

  private final UserService userService;

  @Override
  public ResponseEntity<BaseResponse<UserResponse>> signUp(SignUpRequest request) {

    UserResponse response = userService.signUp(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BaseResponse.success(201, "회원가입이 완료되었습니다.", response));
  }

  @Override
  public ResponseEntity<BaseResponse<List<UserDetailResponse>>> getAllUsers() {

    return ResponseEntity.ok(BaseResponse.success(userService.getAllUsers()));
  }

  @Override
  public ResponseEntity<BaseResponse<UserResponse>> getUserDetail() {

    return ResponseEntity.ok(BaseResponse.success(userService.getUserDetail()));
  }

  @Override
  public ResponseEntity<BaseResponse<Void>> deleteUser(HttpServletResponse response) {

    userService.deleteUser();
    removeRefreshTokenCookie(response);

    return ResponseEntity.ok(BaseResponse.success());
  }

  private void removeRefreshTokenCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie("REFRESH_TOKEN", null);
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }
}
