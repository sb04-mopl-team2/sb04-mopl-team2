package com.codeit.mopl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// https://tychejin.tistory.com/431
@Slf4j
@Configuration
public class OpenSearchConfig {

  private static final String SCHEME = "http";

  // local vd prod 에 다른 값 주입
  @Value("${spring.elasticsearch.port}")
  private static int PORT;
  @Value("${spring.elasticsearch.host}")
  private static String HOST;
  @Value("${spring.elasticsearch.username}")
  private static String USERNAME;
  @Value("${spring.elasticsearch.password}")
  private static String PASSWORD;

  /**
   * OpenSearchClient Bean 설정
   *
   * @return OpenSearchClient
   */
  @Bean
  public OpenSearchClient openSearchClient() {

    final HttpHost httpHost = new HttpHost(SCHEME, HOST, PORT);

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(mapper);

    // 인증 정보를 설정
    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(new AuthScope(httpHost),
        new UsernamePasswordCredentials(USERNAME, PASSWORD.toCharArray()));

    // OpenSearch와 통신하기 위한 OpenSearchTransport 객체를 생성
    final OpenSearchTransport transport =
        ApacheHttpClient5TransportBuilder.builder(httpHost)
            .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                .setDefaultCredentialsProvider(credentialsProvider))
            .setMapper(jsonpMapper)
            .build();

    OpenSearchClient client = new OpenSearchClient(transport);
    // index 생성
    String indexName = "content";
    try {
      boolean indexExists = client.indices().exists(e -> e.index(indexName)).value();
      if (!indexExists) {
        CreateIndexRequest request = new CreateIndexRequest.Builder()
            .index(indexName)
            // 매핑 추가
            .build();
        client.indices().create(request);
        log.info("[OpenSearchConfig] 인덱스 생성 완료- indexName = {}", indexName);
      } else {
        log.info("[OpenSearchConfig] 인덱스 이미 존재함 - indexName = {}", indexName);
      }
    } catch (IOException e) {
      log.error("[OpenSearchConfig] 인덱스 생성 실패 - indexName = {}", indexName);
    }
    return client;
  }
}
