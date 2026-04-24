/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.redis;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Redis 공통 유틸리티 클래스
 *
 * <p>- String 기반 Redis 데이터 접근을 단순화하기 위한 헬퍼 클래스 - JWT, RefreshToken, 인증 코드, 카운터, 플래그 값 저장에 사용
 *
 * <p>특징: - StringRedisTemplate 사용 (직렬화 이슈 없음) - Redis의 atomic 연산(increment/decrement) 활용 - TTL(만료
 * 시간) 지원
 */
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisUtil {

  /**
   * String 전용 RedisTemplate
   *
   * <p>- Key / Value 모두 String으로 처리 - 토큰, 숫자 카운터, 상태 값 저장에 적합
   */
  private final StringRedisTemplate template;

  /**
   * Redis에 저장된 데이터 조회
   *
   * @param key Redis Key
   * @return 저장된 값 (없으면 null)
   */
  public String getData(String key) {

    ValueOperations<String, String> valueOperations = template.opsForValue();
    return valueOperations.get(key);
  }

  /**
   * Redis Key 존재 여부 확인
   *
   * @param key Redis Key
   * @return Key 존재 시 true, 없으면 false
   */
  public boolean existData(String key) {

    return template.hasKey(key);
  }

  /**
   * Redis 데이터 저장 (만료 시간 없음)
   *
   * <p>- 명시적으로 삭제하지 않는 한 유지됨
   *
   * @param key Redis Key
   * @param value 저장할 값
   */
  public void setData(String key, String value) {

    ValueOperations<String, String> valueOperations = template.opsForValue();
    valueOperations.set(key, value);
  }

  /**
   * Redis 데이터 저장 + 만료 시간 설정
   *
   * <p>- RefreshToken, 인증 코드 등 임시 데이터에 사용
   *
   * @param key Redis Key
   * @param value 저장할 값
   * @param duration 만료 시간 (초 단위)
   */
  public void setData(String key, String value, long duration) {

    ValueOperations<String, String> valueOperations = template.opsForValue();
    Duration expireDuration = Duration.ofSeconds(duration);
    valueOperations.set(key, value, expireDuration);
  }

  /**
   * Redis 데이터 삭제
   *
   * @param key Redis Key
   */
  public void deleteData(String key) {

    template.delete(key);
  }

  /**
   * Redis 값 증가 (Atomic 연산)
   *
   * <p>- Redis 내부에서 원자적으로 처리됨 - 동시성 환경에서도 안전
   *
   * @param key Redis Key
   * @return 증가된 값
   */
  public Long increment(String key) {

    return template.opsForValue().increment(key);
  }

  /**
   * Redis 값 감소 (Atomic 연산)
   *
   * @param key Redis Key
   * @return 감소된 값
   */
  public Long decrement(String key) {

    return template.opsForValue().decrement(key);
  }

  /**
   * Redis에 저장된 값을 long 타입으로 조회
   *
   * <p>- 값이 없거나 숫자가 아닐 경우 0 반환 - 카운터, 시도 횟수 제한 등에 사용
   *
   * @param key Redis Key
   * @return long 값 (없거나 파싱 실패 시 0)
   */
  public long getLongValue(String key) {

    try {
      String value = getData(key);
      return value != null ? Long.parseLong(value) : 0L;
    } catch (NumberFormatException e) {
      return 0L;
    }
  }
}
