package com.codeit.mopl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories // Elasticsearch Repository 활성화를 위한 어노테이션
public class ElasticsearchConfig extends ElasticsearchConfiguration {

  @Value("${spring.elasticsearch.uris}")
  private String host;

  @Override
  public ClientConfiguration clientConfiguration() {
    return ClientConfiguration.builder()
//        .connectedTo(host)
        .connectedToLocalhost() // localhost
        .build();
  }
}
