package com.codeit.mopl.util;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Testcontainers
@SpringBootTest
public abstract class IntegrationTestBase {

    // Redis
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7.2")
                    .withExposedPorts(6379);

    // Kafka
    static final KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
                            .asCompatibleSubstituteFor("apache/kafka")
            );

    // OpenSearch
    static final GenericContainer<?> openSearch =
            new GenericContainer<>("opensearchproject/opensearch:2.11.0")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("DISABLE_SECURITY_PLUGIN", "true")
                    .withEnv("plugins.security.disabled", "true")
                    .withExposedPorts(9200)
                    .withStartupTimeout(Duration.ofMinutes(2));

    static {
        redis.start();
        kafka.start();
        openSearch.start();
    }

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
