/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.client;

import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sku.aissue.domain.exception.AnalysisErrorCode;
import com.sku.aissue.exception.CustomException;
import com.sku.aissue.global.config.OpenAiProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OpenAiClient {

  private final WebClient openAiWebClient;
  private final OpenAiProperties props;

  public OpenAiClient(
      @Qualifier("openAiWebClient") WebClient openAiWebClient, OpenAiProperties props) {
    this.openAiWebClient = openAiWebClient;
    this.props = props;
  }

  public String chat(String systemPrompt, String userPrompt) {
    ChatRequest request =
        new ChatRequest(
            props.getModel(),
            List.of(
                new ChatRequest.Message("system", systemPrompt),
                new ChatRequest.Message("user", userPrompt)),
            new ChatRequest.ResponseFormat("json_object"),
            0.8);

    log.info("OpenAI API 요청 시작 - model: {}", props.getModel());

    try {
      ChatResponse response =
          openAiWebClient
              .post()
              .uri("/v1/chat/completions")
              .bodyValue(request)
              .retrieve()
              .bodyToMono(ChatResponse.class)
              .block();

      if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
        log.info("OpenAI API 응답 없음");
        throw new CustomException(AnalysisErrorCode.AI_API_ERROR);
      }

      String content = response.getChoices().get(0).getMessage().getContent();
      log.info("OpenAI API 요청 성공");
      return content;

    } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
      log.info(
          "OpenAI API 호출 실패 - status: {}, body: {}",
          e.getStatusCode(),
          e.getResponseBodyAsString());
      throw new CustomException(AnalysisErrorCode.AI_API_ERROR);
    } catch (WebClientException e) {
      log.info("OpenAI API 호출 실패 - error: {}", e.getMessage());
      throw new CustomException(AnalysisErrorCode.AI_API_ERROR);
    }
  }

  public byte[] generateImage(String prompt) {
    ImageRequest request = new ImageRequest("gpt-image-1", prompt, 1, "1024x1024");

    log.info("gpt-image-1 이미지 생성 요청 시작");

    try {
      ImageResponse response =
          openAiWebClient
              .post()
              .uri("/v1/images/generations")
              .bodyValue(request)
              .retrieve()
              .bodyToMono(ImageResponse.class)
              .block();

      if (response == null || response.getData() == null || response.getData().isEmpty()) {
        log.info("gpt-image-1 응답 없음");
        throw new CustomException(AnalysisErrorCode.AI_API_ERROR);
      }

      String b64 = response.getData().get(0).getB64Json();
      log.info("gpt-image-1 이미지 생성 성공");
      return Base64.getDecoder().decode(b64);

    } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
      log.error(
          "gpt-image-1 호출 실패 - status: {}, body: {}",
          e.getStatusCode(),
          e.getResponseBodyAsString());
      throw new CustomException(AnalysisErrorCode.AI_API_ERROR);
    } catch (WebClientException e) {
      log.error("gpt-image-1 호출 실패 - error: {}", e.getMessage());
      throw new CustomException(AnalysisErrorCode.AI_API_ERROR);
    }
  }

  @Getter
  static class ChatRequest {
    private final String model;
    private final List<Message> messages;

    @JsonProperty("response_format")
    private final ResponseFormat responseFormat;

    private final double temperature;

    ChatRequest(
        String model, List<Message> messages, ResponseFormat responseFormat, double temperature) {
      this.model = model;
      this.messages = messages;
      this.responseFormat = responseFormat;
      this.temperature = temperature;
    }

    @Getter
    static class Message {
      private final String role;
      private final String content;

      Message(String role, String content) {
        this.role = role;
        this.content = content;
      }
    }

    @Getter
    static class ResponseFormat {
      private final String type;

      ResponseFormat(String type) {
        this.type = type;
      }
    }
  }

  @Getter
  @NoArgsConstructor
  static class ChatResponse {
    private List<Choice> choices;

    @Getter
    @NoArgsConstructor
    static class Choice {
      private Message message;

      @Getter
      @NoArgsConstructor
      static class Message {
        private String content;
      }
    }
  }

  @Getter
  static class ImageRequest {
    private final String model;
    private final String prompt;
    private final int n;
    private final String size;

    ImageRequest(String model, String prompt, int n, String size) {
      this.model = model;
      this.prompt = prompt;
      this.n = n;
      this.size = size;
    }
  }

  @Getter
  @NoArgsConstructor
  static class ImageResponse {
    private List<ImageData> data;

    @Getter
    @NoArgsConstructor
    static class ImageData {
      @JsonProperty("b64_json")
      private String b64Json;
    }
  }
}
