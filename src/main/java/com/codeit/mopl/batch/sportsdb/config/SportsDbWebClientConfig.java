package com.codeit.mopl.batch.sportsdb.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SportsDbWebClientConfig {

  @Value("${sportsdb.base-url}")
  private String BASE_URL;

  @Value("${sportsdb.api.key}")
  private String API_KEY;

  @Bean
  @Qualifier("sportsDbWebClient")
  public WebClient sportsDbWebClient(WebClient.Builder builder) {
    return builder
        .baseUrl(BASE_URL)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}