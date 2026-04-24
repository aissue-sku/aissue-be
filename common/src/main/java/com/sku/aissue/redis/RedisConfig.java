/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.RequiredArgsConstructor;

/**
 * Redis 설정 클래스
 *
 * <p>- Redis 연결 정보 설정 - RedisTemplate 직렬화 전략 정의
 *
 * <p>실무 기준: - Key는 문자열 - Value는 JSON(Object 직렬화) - JWT, RefreshToken, 캐시 데이터 저장에 적합
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
@RequiredArgsConstructor
public class RedisConfig {

  /** application.yml / application.properties 의 spring.data.redis.* 설정을 자동으로 바인딩한 객체 */
  private final RedisProperties redisProperties;

  /**
   * Redis 연결 팩토리 설정
   *
   * <p>- 단일 Redis 서버(Standalone) 기준 - Lettuce 클라이언트 사용 (Spring Boot 기본) - 비밀번호는 존재할 경우에만 설정
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {

    RedisStandaloneConfiguration redisConfig =
        new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());

    if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
      redisConfig.setPassword(redisProperties.getPassword());
    }

    return new LettuceConnectionFactory(redisConfig);
  }

  /**
   * RedisTemplate 설정
   *
   * <p>실무 권장 직렬화 전략: - Key / HashKey : StringRedisSerializer - Value / HashValue : JSON 직렬화
   * (GenericJackson2JsonRedisSerializer)
   *
   * <p>이유: - Key는 사람이 읽을 수 있어야 디버깅이 쉬움 - Value는 DTO / Object 저장을 위해 JSON 필요
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(
      RedisConnectionFactory redisConnectionFactory) {

    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory);
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    redisTemplate.afterPropertiesSet();

    return redisTemplate;
  }
}
