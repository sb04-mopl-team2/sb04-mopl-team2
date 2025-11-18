package com.codeit.mopl.domain.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mopl.domain.auth.service.AuthService;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.security.jwt.JwtInformation;
import com.codeit.mopl.security.jwt.JwtRegistry;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import com.codeit.mopl.security.config.TestSecurityConfig;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtRegistry jwtRegistry;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("CSRF 토큰 요청 시 203 상태 코드를 반환한다")
    void getCsrfToken_Success() throws Exception {
        // given
        CsrfToken mockToken = mock(CsrfToken.class);
        when(mockToken.getToken()).thenReturn("test-csrf-token");

        // when & then
        mockMvc.perform(get("/api/auth/csrf-token")
                .requestAttr(CsrfToken.class.getName(), mockToken))
            .andExpect(status().isNonAuthoritativeInformation());
    }

    @Test
    @DisplayName("CSRF 토큰이 null이어도 정상 처리된다")
    void getCsrfToken_WithNullToken() throws Exception {
        // given
        CsrfToken mockToken = mock(CsrfToken.class);
        when(mockToken.getToken()).thenReturn(null);

        // when & then
        mockMvc.perform(get("/api/auth/csrf-token")
                .requestAttr(CsrfToken.class.getName(), mockToken))
            .andExpect(status().isNonAuthoritativeInformation());
    }

    @Test
    @DisplayName("유효한 RefreshToken으로 AccessToken 재발급에 성공한다")
    void reissueToken_Success() throws Exception {
        // given
        String refreshToken = "valid-refresh-token";
        UUID userId = UUID.randomUUID();
        String userEmail = "test@example.com";
        
        Map<String, Object> claims = Map.of(
            "sub", userEmail,
            "exp", new Date(System.currentTimeMillis() + 3600000)
        );
        
        UserDto userDto = new UserDto(userId, userEmail, "Test User", null, false, "USER");
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        
        when(jwtTokenProvider.getClaims(refreshToken)).thenReturn(claims);
        when(userService.findByEmail(userEmail)).thenReturn(userDto);
        when(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).thenReturn(true);
        when(authService.reissueAccessToken(claims, userDto)).thenReturn(newAccessToken);
        when(authService.reissueRefreshToken(claims, userDto)).thenReturn(newRefreshToken);
        when(jwtTokenProvider.getRefreshTokenExpirationMinutes()).thenReturn(10080);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("REFRESH_TOKEN", refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userDto.email").value(userEmail))
            .andExpect(jsonPath("$.accessToken").value(newAccessToken))
            .andExpect(cookie().exists("REFRESH_TOKEN"))
            .andExpect(cookie().value("REFRESH_TOKEN", newRefreshToken));

        verify(jwtRegistry).rotateJwtInformation(eq(refreshToken), any(JwtInformation.class));
    }

    @Test
    @DisplayName("만료된 RefreshToken으로 재발급 시 예외가 발생한다")
    void reissueToken_ExpiredRefreshToken() throws Exception {
        // given
        String expiredToken = "expired-refresh-token";
        String userEmail = "test@example.com";
        
        Map<String, Object> claims = Map.of(
            "sub", userEmail,
            "exp", new Date(System.currentTimeMillis() + 3600000)
        );
        
        UserDto userDto = new UserDto(UUID.randomUUID(), userEmail, "Test User", null, false, "USER");
        
        when(jwtTokenProvider.getClaims(expiredToken)).thenReturn(claims);
        when(userService.findByEmail(userEmail)).thenReturn(userDto);
        when(jwtRegistry.hasActiveJwtInformationByRefreshToken(expiredToken)).thenReturn(false);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("REFRESH_TOKEN", expiredToken)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("RefreshToken 쿠키가 없으면 400 에러가 발생한다")
    void reissueToken_MissingRefreshToken() throws Exception {
        // when & then
        mockMvc.perform(post("/api/auth/refresh"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 사용자 이메일로 재발급 시 예외가 발생한다")
    void reissueToken_InvalidUser() throws Exception {
        // given
        String refreshToken = "valid-refresh-token";
        String invalidEmail = "invalid@example.com";
        
        Map<String, Object> claims = Map.of(
            "sub", invalidEmail,
            "exp", new Date(System.currentTimeMillis() + 3600000)
        );
        
        when(jwtTokenProvider.getClaims(refreshToken)).thenReturn(claims);
        when(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).thenReturn(true);
        when(userService.findByEmail(invalidEmail)).thenThrow(new RuntimeException("User not found"));

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("REFRESH_TOKEN", refreshToken)))
            .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("유효하지 않은 RefreshToken 형식으로 재발급 시 예외가 발생한다")
    void reissueToken_InvalidTokenFormat() throws Exception {
        // given
        String invalidToken = "invalid-token-format";
        
        when(jwtTokenProvider.getClaims(invalidToken)).thenThrow(new IllegalArgumentException("Invalid token"));

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("REFRESH_TOKEN", invalidToken)))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("토큰 재발급 시 쿠키 설정이 올바르게 적용된다")
    void reissueToken_CookieConfiguration() throws Exception {
        // given
        String refreshToken = "valid-refresh-token";
        UUID userId = UUID.randomUUID();
        String userEmail = "test@example.com";
        
        Map<String, Object> claims = Map.of(
            "sub", userEmail,
            "exp", new Date(System.currentTimeMillis() + 3600000)
        );
        
        UserDto userDto = new UserDto(userId, userEmail, "Test User", null, false, "USER");
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        int expirationMinutes = 10080; // 7 days
        
        when(jwtTokenProvider.getClaims(refreshToken)).thenReturn(claims);
        when(userService.findByEmail(userEmail)).thenReturn(userDto);
        when(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).thenReturn(true);
        when(authService.reissueAccessToken(claims, userDto)).thenReturn(newAccessToken);
        when(authService.reissueRefreshToken(claims, userDto)).thenReturn(newRefreshToken);
        when(jwtTokenProvider.getRefreshTokenExpirationMinutes()).thenReturn(expirationMinutes);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("REFRESH_TOKEN", refreshToken)))
            .andExpect(status().isOk())
            .andExpect(cookie().maxAge("REFRESH_TOKEN", expirationMinutes * 60))
            .andExpect(cookie().path("REFRESH_TOKEN", "/"));
    }
}