package com.codeit.mopl.oauth.utils;

import com.codeit.mopl.oauth.utils.access_token.TestAccessTokens;
import com.codeit.mopl.oauth.utils.clientRegistration.TestClientRegistrations;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;

public class TestOAuth2UserRequests {

    public static OAuth2UserRequest googleRequest() {
        return new OAuth2UserRequest(
                TestClientRegistrations.google(),
                TestAccessTokens.googleAccessToken()
        );
    }

    public static OAuth2UserRequest kakaoRequest() {
        return new OAuth2UserRequest(
                TestClientRegistrations.kakao(),
                TestAccessTokens.kakaoAccessToken()
        );
    }

    public static OAuth2UserRequest githubRequest() {
        return new OAuth2UserRequest(
                TestClientRegistrations.github(),
                TestAccessTokens.githubAccessToken()
        );
    }
}