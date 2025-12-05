package com.codeit.mopl.oauth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.google")
public record GoogleClientProperties(
        String clientId,
        String clientSecret,
        String redirectUri
) {}