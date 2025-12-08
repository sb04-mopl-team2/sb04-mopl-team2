package com.codeit.mopl.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Slf4j
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.codeit.mopl.search")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

  @Override
  public ClientConfiguration clientConfiguration() {
    log.info("Configuring Elasticsearch client for localhost:9200");

    return ClientConfiguration.builder()
        .connectedTo("localhost:9200")
        .build();
  }
}
