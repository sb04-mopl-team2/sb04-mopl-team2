package com.codeit.mopl.oatuh.utils.oauth2_user;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

public class TestOAuth2Users {

    public static OAuth2User googleUser() {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                googleAttributes(),
                "sub"
        );
    }
    public static OAuth2User kakaoUser() {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                kakaoAttributes(),
                "id"
        );
    }

    public static OAuth2User githubUser() {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                githubAttributes(),
                "id"
        );
    }

    private static Map<String, Object> googleAttributes() {
        return Map.of(
                "sub", "109876543210987654321",
                "name", "홍길동",
                "given_name", "길동",
                "family_name", "홍",
                "picture", "https://example.com/profile.jpg",
                "email", "test@gmail.com",
                "email_verified", true,
                "locale", "ko"
        );
    }

    private static Map<String, Object> kakaoAttributes() {
        return Map.of(
                "id", "9876543210",
                "properties", Map.of(
                        "nickname", "홍길동",
                        "profile_image", "https://example.com/kakao-profile.jpg"
                )
        );
    }

    private static Map<String, Object> githubAttributes() {
        return Map.of(
                "id", "12345678",
                "login", "honggildong",
                "name", "Hong Gil-dong",
                "avatar_url", "https://avatars.githubusercontent.com/u/12345678?v=4",
                "email", "test@github.com"
        );
    }
}
