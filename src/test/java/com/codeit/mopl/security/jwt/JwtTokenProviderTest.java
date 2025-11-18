package com.codeit.mopl.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String testSecretKey;
    private int accessTokenExpiration = 60; // 60 minutes
    private int refreshTokenExpiration = 10080; // 7 days

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        
        testSecretKey = "test-secret-key-for-jwt-token-generation-minimum-256-bits-required";
        
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpirationMinutes", accessTokenExpiration);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMinutes", refreshTokenExpiration);
    }

    @Test
    @DisplayName("AccessToken 생성에 성공한다")
    void generateAccessToken_Success() {
        // given
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", UUID.randomUUID().toString());
        claims.put("roles", "USER");
        String subject = "test@example.com";

        // when
        String token = jwtTokenProvider.generateAccessToken(claims, subject);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("RefreshToken 생성에 성공한다")
    void generateRefreshToken_Success() {
        // given
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", UUID.randomUUID().toString());
        String subject = "test@example.com";

        // when
        String token = jwtTokenProvider.generateRefreshToken(claims, subject);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("생성한 토큰에서 Claims를 추출할 수 있다")
    void getClaims_Success() {
        // given
        UUID userId = UUID.randomUUID();
        Map<String, Object> originalClaims = new HashMap<>();
        originalClaims.put("userId", userId.toString());
        originalClaims.put("roles", "USER");
        String subject = "test@example.com";

        String token = jwtTokenProvider.generateAccessToken(originalClaims, subject);

        // when
        Map<String, Object> extractedClaims = jwtTokenProvider.getClaims(token);

        // then
        assertThat(extractedClaims).isNotNull();
        assertThat(extractedClaims.get("sub")).isEqualTo(subject);
        assertThat(extractedClaims.get("userId")).isEqualTo(userId.toString());
        assertThat(extractedClaims.get("roles")).isEqualTo("USER");
    }

    @Test
    @DisplayName("유효하지 않은 토큰에서 Claims 추출 시 예외가 발생한다")
    void getClaims_InvalidToken() {
        // given
        String invalidToken = "invalid.token.here";

        // when & then
        assertThatThrownBy(() -> jwtTokenProvider.getClaims(invalidToken))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JWT 파싱 실패");
    }

    @Test
    @DisplayName("토큰에서 이메일을 추출할 수 있다")
    void getEmail_Success() throws ParseException {
        // given
        String subject = "test@example.com";
        Map<String, Object> claims = new HashMap<>();
        String token = jwtTokenProvider.generateAccessToken(claims, subject);

        // when
        String extractedEmail = jwtTokenProvider.getEmail(token);

        // then
        assertThat(extractedEmail).isEqualTo(subject);
    }

    @Test
    @DisplayName("토큰에서 사용자 ID를 추출할 수 있다")
    void getUserId_Success() throws ParseException {
        // given
        UUID userId = UUID.randomUUID();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        String token = jwtTokenProvider.generateAccessToken(claims, "test@example.com");

        // when
        UUID extractedUserId = jwtTokenProvider.getUserId(token);

        // then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("만료되지 않은 토큰은 유효하다")
    void isExpired_ValidToken() {
        // given
        Map<String, Object> claims = new HashMap<>();
        String token = jwtTokenProvider.generateAccessToken(claims, "test@example.com");

        // when
        boolean expired = jwtTokenProvider.isExpired(token);

        // then
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("JWS 검증이 성공한다")
    void verifyJws_Success() {
        // given
        Map<String, Object> claims = new HashMap<>();
        String token = jwtTokenProvider.generateAccessToken(claims, "test@example.com");

        // when & then - should not throw exception
        jwtTokenProvider.verifyJws(token);
    }

    @Test
    @DisplayName("유효하지 않은 JWS 검증 시 예외가 발생한다")
    void verifyJws_InvalidToken() {
        // given
        String invalidToken = "invalid.token.here";

        // when & then
        assertThatThrownBy(() -> jwtTokenProvider.verifyJws(invalidToken))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("JWT 검증 실패");
    }

    @Test
    @DisplayName("AccessToken과 RefreshToken의 만료 시간이 다르다")
    void tokenExpirationDifference() {
        // given
        Map<String, Object> claims = new HashMap<>();
        String subject = "test@example.com";

        String accessToken = jwtTokenProvider.generateAccessToken(claims, subject);
        String refreshToken = jwtTokenProvider.generateRefreshToken(claims, subject);

        // when
        Map<String, Object> accessClaims = jwtTokenProvider.getClaims(accessToken);
        Map<String, Object> refreshClaims = jwtTokenProvider.getClaims(refreshToken);

        Date accessExp = (Date) accessClaims.get("exp");
        Date refreshExp = (Date) refreshClaims.get("exp");

        // then
        assertThat(refreshExp).isAfter(accessExp);
    }

    @Test
    @DisplayName("토큰 타입이 올바르게 설정된다")
    void tokenType_SetCorrectly() {
        // given
        Map<String, Object> claims = new HashMap<>();
        String subject = "test@example.com";

        String accessToken = jwtTokenProvider.generateAccessToken(claims, subject);
        String refreshToken = jwtTokenProvider.generateRefreshToken(claims, subject);

        // when
        Map<String, Object> accessClaims = jwtTokenProvider.getClaims(accessToken);
        Map<String, Object> refreshClaims = jwtTokenProvider.getClaims(refreshToken);

        // then
        assertThat(accessClaims.get("type")).isEqualTo("access");
        assertThat(refreshClaims.get("type")).isEqualTo("refresh");
    }

    @Test
    @DisplayName("각 토큰은 고유한 JWT ID를 가진다")
    void tokenHasUniqueJwtId() {
        // given
        Map<String, Object> claims = new HashMap<>();
        String subject = "test@example.com";

        String token1 = jwtTokenProvider.generateAccessToken(claims, subject);
        String token2 = jwtTokenProvider.generateAccessToken(claims, subject);

        // when
        Map<String, Object> claims1 = jwtTokenProvider.getClaims(token1);
        Map<String, Object> claims2 = jwtTokenProvider.getClaims(token2);

        // then
        assertThat(claims1.get("jti")).isNotNull();
        assertThat(claims2.get("jti")).isNotNull();
        assertThat(claims1.get("jti")).isNotEqualTo(claims2.get("jti"));
    }

    @Test
    @DisplayName("토큰 만료 시간 getter가 올바른 값을 반환한다")
    void getExpirationMinutes() {
        // when
        int accessExpiration = jwtTokenProvider.getAccessTokenExpirationMinutes();
        int refreshExpiration = jwtTokenProvider.getRefreshTokenExpirationMinutes();

        // then
        assertThat(accessExpiration).isEqualTo(60);
        assertThat(refreshExpiration).isEqualTo(10080);
    }
}