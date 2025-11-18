package com.codeit.mopl.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("유효한 claims로 AccessToken을 재발급한다")
    void reissueAccessToken_Success() {
        // given
        String userEmail = "test@example.com";
        String expectedToken = "new-access-token";
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userEmail);
        claims.put("exp", new Date(System.currentTimeMillis() + 3600000)); // 1 hour from now

        UserDto userDto = new UserDto(UUID.randomUUID(), userEmail, "Test User", null, false, "USER");

        when(jwtTokenProvider.generateAccessToken(claims, userEmail)).thenReturn(expectedToken);

        // when
        String result = authService.reissueAccessToken(claims, userDto);

        // then
        assertThat(result).isEqualTo(expectedToken);
        verify(jwtTokenProvider).generateAccessToken(claims, userEmail);
    }

    @Test
    @DisplayName("만료된 claims로 AccessToken 재발급 시 예외가 발생한다")
    void reissueAccessToken_ExpiredClaims() {
        // given
        String userEmail = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userEmail);
        claims.put("exp", new Date(System.currentTimeMillis() - 3600000)); // 1 hour ago

        UserDto userDto = new UserDto(UUID.randomUUID(), userEmail, "Test User", null, false, "USER");

        // when & then
        assertThatThrownBy(() -> authService.reissueAccessToken(claims, userDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("RefreshToken");
    }

    @Test
    @DisplayName("유효한 claims로 RefreshToken을 재발급한다")
    void reissueRefreshToken_Success() {
        // given
        String userEmail = "test@example.com";
        String expectedToken = "new-refresh-token";
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userEmail);
        claims.put("exp", new Date(System.currentTimeMillis() + 604800000)); // 7 days from now

        UserDto userDto = new UserDto(UUID.randomUUID(), userEmail, "Test User", null, false, "USER");

        when(jwtTokenProvider.generateRefreshToken(claims, userEmail)).thenReturn(expectedToken);

        // when
        String result = authService.reissueRefreshToken(claims, userDto);

        // then
        assertThat(result).isEqualTo(expectedToken);
        verify(jwtTokenProvider).generateRefreshToken(claims, userEmail);
    }

    @Test
    @DisplayName("만료된 claims로 RefreshToken 재발급 시 예외가 발생한다")
    void reissueRefreshToken_ExpiredClaims() {
        // given
        String userEmail = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userEmail);
        claims.put("exp", new Date(System.currentTimeMillis() - 604800000)); // 7 days ago

        UserDto userDto = new UserDto(UUID.randomUUID(), userEmail, "Test User", null, false, "USER");

        // when & then
        assertThatThrownBy(() -> authService.reissueRefreshToken(claims, userDto))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("RefreshToken");
    }

    @Test
    @DisplayName("exp가 정확히 현재 시간일 때 재발급 시 예외가 발생한다")
    void reissueToken_ExactlyExpired() {
        // given
        String userEmail = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userEmail);
        claims.put("exp", new Date(System.currentTimeMillis() - 1)); // just expired

        UserDto userDto = new UserDto(UUID.randomUUID(), userEmail, "Test User", null, false, "USER");

        // when & then
        assertThatThrownBy(() -> authService.reissueAccessToken(claims, userDto))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("여러 사용자에 대해 동시에 토큰을 재발급할 수 있다")
    void reissueToken_MultipleUsers() {
        // given
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";
        
        Map<String, Object> claims1 = new HashMap<>();
        claims1.put("sub", email1);
        claims1.put("exp", new Date(System.currentTimeMillis() + 3600000));
        
        Map<String, Object> claims2 = new HashMap<>();
        claims2.put("sub", email2);
        claims2.put("exp", new Date(System.currentTimeMillis() + 3600000));

        UserDto user1 = new UserDto(UUID.randomUUID(), email1, "User 1", null, false, "USER");
        UserDto user2 = new UserDto(UUID.randomUUID(), email2, "User 2", null, false, "USER");

        when(jwtTokenProvider.generateAccessToken(claims1, email1)).thenReturn("token1");
        when(jwtTokenProvider.generateAccessToken(claims2, email2)).thenReturn("token2");

        // when
        String token1 = authService.reissueAccessToken(claims1, user1);
        String token2 = authService.reissueAccessToken(claims2, user2);

        // then
        assertThat(token1).isEqualTo("token1");
        assertThat(token2).isEqualTo("token2");
    }
}