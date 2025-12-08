package com.codeit.mopl.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.codeit.mopl.search")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

  @Value("${spring.elasticsearch.uris}")
  private String esUri;

  @Override
  public ClientConfiguration clientConfiguration() {
    log.info("[Elasticsearch] 클라이언트 연결 시작, uri = {}", esUri);

    return ClientConfiguration.builder()
        .connectedTo(esUri)
        .build();
  }
}
