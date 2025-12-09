package com.codeit.mopl.batch.tmdb.base.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

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
        .build();
  }
}
