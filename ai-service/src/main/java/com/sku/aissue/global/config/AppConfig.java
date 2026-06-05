/*
 * Copyright (c) unijun0109@gmail.com
 */
package com.sku.aissue.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({
  OpenAiProperties.class,
  QdrantProperties.class,
  ImageStorageProperties.class
})
public class AppConfig {

  @Bean
  public WebClient webClient() {
    return WebClient.builder().build();
  }

  @Bean
  @LoadBalanced
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean("qdrantWebClient")
  public WebClient qdrantWebClient(QdrantProperties props) {
    return WebClient.builder()
        .baseUrl(props.getUrl())
        .defaultHeader(
            org.springframework.http.HttpHeaders.CONTENT_TYPE,
            org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Bean("openAiWebClient")
  public WebClient openAiWebClient(OpenAiProperties props) {
    ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(
                (ClientCodecConfigurer configurer) ->
                    configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    return WebClient.builder()
        .baseUrl(props.getBaseUrl())
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getKey())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .exchangeStrategies(strategies)
        .build();
  }
}
