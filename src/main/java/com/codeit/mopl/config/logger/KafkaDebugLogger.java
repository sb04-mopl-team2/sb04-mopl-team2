package com.codeit.mopl.config.logger;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaDebugLogger {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @PostConstruct
  public void logKafkaBootstrapServers() {
    Map<String, Object> props =
        kafkaTemplate.getProducerFactory().getConfigurationProperties();

    log.info("[Kafka Debug] bootstrap.servers = {}", props.get("bootstrap.servers"));
    log.info("[Kafka Debug] security.protocol = {}", props.get("security.protocol"));
    log.info("[Kafka Debug] sasl.mechanism = {}", props.get("sasl.mechanism"));
    log.info("[Kafka Debug] client.id = {}", props.get("client.id"));
  }
}
