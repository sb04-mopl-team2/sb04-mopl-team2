package com.codeit.mopl.oauth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.kakao")
public record KakaoClientProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String authorizationGrantType,
        String clientAuthenticationMethod,
        String clientName,
        List<String> scope
) {
}
