package com.codeit.mopl.oauth.service;

import com.codeit.mopl.domain.user.entity.Provider;
import com.codeit.mopl.exception.user.NotSupportedSocialLoginException;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.oauth.userInfo.GoogleUserInfo;
import com.codeit.mopl.oauth.userInfo.KakaoUserInfo;
import com.codeit.mopl.oauth.userInfo.OAuth2UserInfo;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {
    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = loadUserFromParent(userRequest);

        OAuth2UserInfo oAuth2UserInfo = null;
        if (userRequest.getClientRegistration().getRegistrationId().equals("google")) {
            oAuth2UserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        } else if (userRequest.getClientRegistration().getRegistrationId().equals("kakao")) {
            oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        } else {
                log.error("[소셜 로그인] 지원하지 않는 로그인");
                throw new NotSupportedSocialLoginException(UserErrorCode.NOT_SUPPORTED_SOCIAL_LOGIN, Map.of("site",userRequest.getClientRegistration().getRegistrationId()));
        }

        String email = oAuth2UserInfo.getProviderEmail();
        String name = oAuth2UserInfo.getProviderName();
        String profileImageUrl = oAuth2UserInfo.getProfileImageUrl();
        Provider provider = oAuth2UserInfo.getProvider();
        UserDto user = userService.findOrCreateOAuth2User(email, name, profileImageUrl,provider);
        return new CustomUserDetails(user,oAuth2User.getAttributes());
    }

    // Test 가능하도록 메서드 분리 (super.loadUser 테스트 mocking 불가 함)
    public OAuth2User loadUserFromParent(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        return super.loadUser(userRequest);
    }
}
