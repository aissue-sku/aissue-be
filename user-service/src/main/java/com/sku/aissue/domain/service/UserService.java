/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.util.List;

import com.sku.aissue.domain.dto.request.SignUpRequest;
import com.sku.aissue.domain.dto.response.UserDetailResponse;
import com.sku.aissue.domain.dto.response.UserResponse;

public interface UserService {

  UserResponse signUp(SignUpRequest request);

  List<UserDetailResponse> getAllUsers();

  UserResponse getUserDetail();

  void deleteUser();
}
