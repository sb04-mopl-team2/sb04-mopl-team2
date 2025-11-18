package com.codeit.mopl.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.exception.auth.InvalidTokenException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtRegistryTest {

    private JwtRegistry jwtRegistry;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        jwtRegistry = new JwtRegistry(jwtTokenProvider);
    }

    @Test
    @DisplayName("JWT 정보를 등록하고 조회할 수 있다")
    void registerAndRetrieveJwtInformation() {
        // given
        UserDto userDto = new UserDto(UUID.randomUUID(), "test@example.com", "Test User", null, false, "USER");
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        JwtInformation jwtInfo = new JwtInformation(userDto, accessToken, refreshToken);

        // when
        jwtRegistry.registerJwtInformation(jwtInfo);

        // then
        assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken(accessToken)).isTrue();
        assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).isTrue();
        assertThat(jwtRegistry.hasActiveJwtInformationByUserId(userDto.id())).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 AccessToken은 false를 반환한다")
    void hasActiveJwtInformation_NonExistentAccessToken() {
        // when
        boolean result = jwtRegistry.hasActiveJwtInformationByAccessToken("non-existent-token");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 RefreshToken은 false를 반환한다")
    void hasActiveJwtInformation_NonExistentRefreshToken() {
        // when
        boolean result = jwtRegistry.hasActiveJwtInformationByRefreshToken("non-existent-token");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("사용자 ID로 JWT 정보를 무효화할 수 있다")
    void invalidateJwtInformationByUserId() {
        // given
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "test@example.com", "Test User", null, false, "USER");
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        JwtInformation jwtInfo = new JwtInformation(userDto, accessToken, refreshToken);

        jwtRegistry.registerJwtInformation(jwtInfo);

        // when
        jwtRegistry.invalidateJwtInformationByUserId(userId);

        // then
        assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken(accessToken)).isFalse();
        assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)).isFalse();
        assertThat(jwtRegistry.hasActiveJwtInformationByUserId(userId)).isFalse();
    }

    @Test
    @DisplayName("JWT 정보를 로테이션할 수 있다")
    void rotateJwtInformation() {
        // given
        UserDto userDto = new UserDto(UUID.randomUUID(), "test@example.com", "Test User", null, false, "USER");
        String oldAccessToken = "old-access-token";
        String oldRefreshToken = "old-refresh-token";
        JwtInformation oldJwtInfo = new JwtInformation(userDto, oldAccessToken, oldRefreshToken);

        jwtRegistry.registerJwtInformation(oldJwtInfo);

        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        JwtInformation newJwtInfo = new JwtInformation(userDto, newAccessToken, newRefreshToken);

        // when
        jwtRegistry.rotateJwtInformation(oldRefreshToken, newJwtInfo);

        // then
        assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken(oldAccessToken)).isFalse();
        assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken(oldRefreshToken)).isFalse();
        assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken(newAccessToken)).isTrue();
        assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken(newRefreshToken)).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 RefreshToken으로 로테이션 시 예외가 발생한다")
    void rotateJwtInformation_NonExistentRefreshToken() {
        // given
        UserDto userDto = new UserDto(UUID.randomUUID(), "test@example.com", "Test User", null, false, "USER");
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        JwtInformation newJwtInfo = new JwtInformation(userDto, newAccessToken, newRefreshToken);

        // when & then
        assertThatThrownBy(() -> jwtRegistry.rotateJwtInformation("non-existent-refresh-token", newJwtInfo))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("최대 활성 JWT 개수를 초과하면 가장 오래된 것이 제거된다")
    void registerJwtInformation_ExceedsMaxCount() {
        // given
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "test@example.com", "Test User", null, false, "USER");
        
        JwtInformation jwtInfo1 = new JwtInformation(userDto, "access-1", "refresh-1");
        JwtInformation jwtInfo2 = new JwtInformation(userDto, "access-2", "refresh-2");

        // when
        jwtRegistry.registerJwtInformation(jwtInfo1);
        jwtRegistry.registerJwtInformation(jwtInfo2); // maxActiveJwtCount = 1

        // then - only the latest should remain
        assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access-1")).isFalse();
        assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access-2")).isTrue();
    }

    @Test
    @DisplayName("만료된 JWT 정보를 정리할 수 있다")
    void clearExpiredJwtInformation() {
        // given
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "test@example.com", "Test User", null, false, "USER");
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        JwtInformation jwtInfo = new JwtInformation(userDto, accessToken, refreshToken);

        jwtRegistry.registerJwtInformation(jwtInfo);
        
        // Mock the token as expired
        when(jwtTokenProvider.isExpired(refreshToken)).thenReturn(true);

        // when
        jwtRegistry.clearExpiredJwtInformation();

        // then
        assertThat(jwtRegistry.hasActiveJwtInformationByUserId(userId)).isFalse();
    }

    @Test
    @DisplayName("만료되지 않은 JWT는 정리되지 않는다")
    void clearExpiredJwtInformation_NonExpired() {
        // given
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, "test@example.com", "Test User", null, false, "USER");
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        JwtInformation jwtInfo = new JwtInformation(userDto, accessToken, refreshToken);

        jwtRegistry.registerJwtInformation(jwtInfo);
        
        // Mock the token as not expired
        when(jwtTokenProvider.isExpired(refreshToken)).thenReturn(false);

        // when
        jwtRegistry.clearExpiredJwtInformation();

        // then
        assertThat(jwtRegistry.hasActiveJwtInformationByUserId(userId)).isTrue();
        assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken(accessToken)).isTrue();
    }
}