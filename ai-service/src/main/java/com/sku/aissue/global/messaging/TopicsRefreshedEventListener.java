/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.sku.aissue.domain.service.IssueCardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicsRefreshedEventListener {

  private final IssueCardService issueCardService;

  @RabbitListener(queues = RabbitMQConfig.QUEUE_TOPICS_REFRESHED)
  public void onTopicsRefreshed(Object ignored) {
    log.info("topics.refreshed 이벤트 수신 - 이슈 카드 생성 시작 (이미지 생성 생략)");
    try {
      issueCardService.generateAndSaveByHotTopics(false);
    } catch (Exception e) {
      log.error("이슈 카드 생성 실패: {}", e.getMessage());
    }
  }
}
