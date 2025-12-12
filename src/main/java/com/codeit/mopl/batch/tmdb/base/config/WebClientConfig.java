package com.codeit.mopl.batch.tmdb.base.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class WebClientConfig {

  @Value("${tmdb.base-url}")
  private String BASE_URL;

  @Value("${tmdb.api.token}")
  private String TOKEN;

  @Bean
  @Qualifier("tmdbWebClient")
  public WebClient webClient(WebClient.Builder builder) {
    return builder
        .baseUrl(BASE_URL)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer "+TOKEN)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .filter((request, next) -> {
          log.info("[TMDB] → Request URL = {}", request.url());
          request.headers().forEach((k, v) -> log.info("[TMDB] → Header {} = {}", k, v));
          return next.exchange(request);
        })
        .build();
  }
}
