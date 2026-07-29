package com.codeit.mopl.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestOpenSearchConfig {

    @Bean
    public OpenSearchClient openSearchClient(
            @Value("${opensearch.host}") String host,
            @Value("${opensearch.port}") int port
    ) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        OpenSearchTransport transport =
                new RestClientTransport(
                        RestClient.builder(new HttpHost("http", host, port)).build(),
                        new JacksonJsonpMapper(mapper)
                );

        return new OpenSearchClient(transport);
    }
}
