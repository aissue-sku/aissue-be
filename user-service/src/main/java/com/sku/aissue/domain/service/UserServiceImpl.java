/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.domain.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sku.aissue.domain.dto.request.SignUpRequest;
import com.sku.aissue.domain.dto.response.UserDetailResponse;
import com.sku.aissue.domain.dto.response.UserResponse;
import com.sku.aissue.domain.entity.User;
import com.sku.aissue.domain.exception.UserErrorCode;
import com.sku.aissue.domain.mapper.UserMapper;
import com.sku.aissue.domain.repository.UserRepository;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.redis.RedisUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  /** Redis Refresh Token 키 접두사 (auth-service의 TokenRepository와 동일한 규칙) */
  private static final String REFRESH_TOKEN_PREFIX = "RT:";

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final RedisUtil redisUtil;

  @Override
  @Transactional
  public UserResponse signUp(SignUpRequest request) {

    if (userRepository.existsByUsername(request.getUsername())) {
      throw new CustomException(UserErrorCode.EXIST_USERNAME);
    }

    try {
      String encodedPassword = passwordEncoder.encode(request.getPassword());
      User user = userMapper.toUser(request, encodedPassword);
      User savedUser = userRepository.save(user);

      log.info("회원가입 성공 - userId: {}, username: {}", savedUser.getId(), savedUser.getUsername());
      return userMapper.toUserResponse(savedUser);
    } catch (DataIntegrityViolationException e) {
      // existsByUsername 체크 이후 동시 요청으로 중복 발생 시 처리
      throw new CustomException(UserErrorCode.EXIST_USERNAME);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserDetailResponse> getAllUsers() {

    List<User> allUsers = userRepository.findAll();
    List<UserDetailResponse> userDetails =
        allUsers.stream().map(userMapper::toUserDetailResponse).toList();

    log.info("전체 사용자 조회, 총 사용자 수: {}", userDetails.size());
    return userDetails;
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse getUserDetail() {

    User user = getCurrentUser();

    log.info("사용자 상세 조회 - userId: {}, username: {}", user.getId(), user.getUsername());
    return userMapper.toUserResponse(user);
  }

  @Transactional
  @Override
  public void deleteUser() {

    User user = getCurrentUser();

    // DB 삭제 먼저 → 성공 후 Redis 삭제 (DB 롤백 시 Redis 불일치 방지)
    userRepository.delete(user);
    redisUtil.deleteData(REFRESH_TOKEN_PREFIX + user.getUsername());

    log.info("사용자 계정 삭제 - userId: {}, username: {}", user.getId(), user.getUsername());
  }

  private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      log.error("인증 실패 - 인증 정보 없음");
      throw new CustomException(UserErrorCode.UNAUTHORIZED);
    }

    Object principal = authentication.getPrincipal();
    String username;

    if (principal instanceof String str) {
      username = str;
    } else {
      log.error("인증 실패 - Principal 타입 알 수 없음: {}", principal.getClass());
      throw new CustomException(UserErrorCode.UNAUTHORIZED);
    }

    if (username.isBlank()) {
      log.error("인증 실패 - 추출된 username이 빈 문자열");
      throw new CustomException(UserErrorCode.UNAUTHORIZED);
    }

    return userRepository
        .findByUsername(username)
        .orElseThrow(
            () -> {
              log.error("사용자 찾기 실패 - username: {}", username);
              return new CustomException(UserErrorCode.USER_NOT_FOUND);
            });
  }
}
