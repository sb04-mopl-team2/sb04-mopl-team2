package com.codeit.mopl.oauth.service;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Provider;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.exception.user.NotSupportedSocialLoginException;
import com.codeit.mopl.oauth.utils.TestOAuth2UserRequests;
import com.codeit.mopl.oauth.utils.oauth2_user.TestOAuth2Users;
import com.codeit.mopl.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

@ExtendWith(MockitoExtension.class)
public class OAuth2ServiceTest {
    @Mock
    private UserService userService;

    @Spy
    @InjectMocks
    private OAuth2UserService oAuth2UserService;

    @DisplayName("Google 소셜 로그인 성공")
    @Test
    void googleOAuth2Test() throws OAuth2AuthenticationException {
        // given
        OAuth2UserRequest request = TestOAuth2UserRequests.googleRequest();
        OAuth2User user = TestOAuth2Users.googleUser();
        willReturn(user)
                .given(oAuth2UserService)
                .loadUserFromParent(any(OAuth2UserRequest.class));
        UserDto userDto = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test@gmail.com", "홍길동","https://example.com/profile.jpg", Role.USER,false);
        given(userService.findOrCreateOAuth2User("test@gmail.com","홍길동","https://example.com/profile.jpg", Provider.GOOGLE)).willReturn(userDto);

        CustomUserDetails details = (CustomUserDetails) oAuth2UserService.loadUser(request);
        assertEquals("홍길동",details.getUser().name());
    }
    @DisplayName("Kakao 소셜 로그인 성공")
    @Test
    void kakaoOAuth2Test() throws OAuth2AuthenticationException {
        // given
        OAuth2UserRequest request = TestOAuth2UserRequests.kakaoRequest();
        OAuth2User user = TestOAuth2Users.kakaoUser();
        willReturn(user)
                .given(oAuth2UserService)
                .loadUserFromParent(any(OAuth2UserRequest.class));
        UserDto userDto = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "홍길동_9876543210@kakao.com", "홍길동","https://example.com/kakao-profile.jpg", Role.USER,false);
        given(userService.findOrCreateOAuth2User("홍길동_9876543210@kakao.com","홍길동","https://example.com/kakao-profile.jpg",Provider.KAKAO)).willReturn(userDto);

        CustomUserDetails details = (CustomUserDetails) oAuth2UserService.loadUser(request);
        assertEquals("홍길동",details.getUser().name());
        assertEquals("홍길동_9876543210@kakao.com",details.getUser().email());
    }

    @DisplayName("지원하지 않는 소셜")
    @Test
    void githubOAuth2Test() throws OAuth2AuthenticationException {
        // given
        OAuth2UserRequest request = TestOAuth2UserRequests.githubRequest();
        OAuth2User user = TestOAuth2Users.githubUser();
        willReturn(user)
                .given(oAuth2UserService)
                .loadUserFromParent(any(OAuth2UserRequest.class));

        NotSupportedSocialLoginException exception = assertThrows(NotSupportedSocialLoginException.class, () -> {
            oAuth2UserService.loadUser(request);
        });
        assertEquals("지원하지 않는 소셜 로그인입니다.",exception.getErrorCode().getMessage());
        assertEquals("github",exception.getDetails().get("site"));
    }
}
