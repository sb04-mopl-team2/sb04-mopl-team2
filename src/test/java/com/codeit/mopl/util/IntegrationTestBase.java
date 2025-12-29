package com.codeit.mopl.util;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
@SpringBootTest
public abstract class IntegrationTestBase {

    // Redis
    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7.2")
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort());

    // Kafka
    @Container
    static final KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
                            .asCompatibleSubstituteFor("apache/kafka")
            );

    // OpenSearch
    @Container
    static final GenericContainer<?> openSearch =
            new GenericContainer<>("opensearchproject/opensearch:2.11.0")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("DISABLE_SECURITY_PLUGIN", "true")
                    .withEnv("plugins.security.disabled", "true")
                    .withExposedPorts(9200)
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .waitingFor(
                            Wait.forHttp("/")
                                    .forPort(9200)
                                    .forStatusCode(200)
                    );

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {

        /* Redis */
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));

        /* Kafka */
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        /* OpenSearch */
        registry.add("opensearch.host", openSearch::getHost);
        registry.add("opensearch.port", () -> openSearch.getMappedPort(9200));
    }
}
