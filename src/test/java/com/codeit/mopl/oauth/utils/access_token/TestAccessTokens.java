package com.codeit.mopl.oauth.utils.access_token;

import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.Set;

public class TestAccessTokens {

    public static OAuth2AccessToken googleAccessToken() {
        Instant now = Instant.now();

        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-google-access-token",   // 임의 토큰 값
                now,
                now.plusSeconds(3600),        // 1시간 후 만료
                Set.of("profile", "email")
        );
    }

    public static OAuth2AccessToken kakaoAccessToken() {
        Instant now = Instant.now();

        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-kakao-access-token",
                now,
                now.plusSeconds(3600),
                Set.of("profile_image","profile_nickname")
        );
    }

    public static OAuth2AccessToken githubAccessToken() {
        Instant now = Instant.now();

        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-github-access-token",
                now,
                now.plusSeconds(3600),
                Set.of("read:user", "user:email") // GitHub OAuth 스코프
        );
    }
}