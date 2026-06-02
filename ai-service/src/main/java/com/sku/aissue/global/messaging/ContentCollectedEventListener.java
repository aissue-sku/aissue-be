/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.messaging;

import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.sku.aissue.domain.service.ArticleEmbeddingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentCollectedEventListener {

  private final ArticleEmbeddingService articleEmbeddingService;

  @RabbitListener(queues = RabbitMQConfig.QUEUE_CONTENT_COLLECTED)
  public void onContentCollected(ContentCollectedEvent event) {
    if (event == null || event.ids() == null || event.ids().isEmpty()) return;
    log.info("content.collected 이벤트 수신 - {}건", event.ids().size());
    try {
      articleEmbeddingService.embedBatch(event.ids());
    } catch (Exception e) {
      log.error("임베딩 처리 실패 - {}건: {}", event.ids().size(), e.getMessage());
    }
  }

  public record ContentCollectedEvent(List<Long> ids) {}
}
