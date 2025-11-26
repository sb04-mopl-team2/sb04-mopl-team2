package com.codeit.mopl.domain.auth.service;

import com.codeit.mopl.domain.auth.dto.request.ResetPasswordRequest;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.auth.InvalidTokenException;
import com.codeit.mopl.mail.service.MailService;
import com.codeit.mopl.mail.utils.PasswordUtils;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordUtils passwordUtils;
    @Mock
    private MailService mailService;

    @InjectMocks
    private AuthService authService;

    @DisplayName("email이 주어졌을 때 존재하는 유저라면 비밀번호를 초기화하고 이메일로 임시 비밀번호를 전송한다")
    @Test
    void resetPasswordShouldSucceedWhenExistsEmail() throws MessagingException {
        // given
        ResetPasswordRequest request = new ResetPasswordRequest("test@test.com");
        User findUser = new User("test@test.com","password","test");
        String tempPw = "TempPW1234";
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(findUser));
        given(passwordUtils.makeTempPassword()).willReturn(tempPw);
        doNothing().when(mailService).sendMail("test@test.com",tempPw);

        // when
        authService.resetPassword(request);

        // then
        verify(mailService).sendMail("test@test.com",tempPw);
    }

    @DisplayName("email이 주어졌는데 존재하지 않는 유저라면 아무 행동도 하지 않는다")
    @Test
    void resetPasswordShouldFailWhenUserNotFoundByEmail() throws MessagingException {
        // given
        ResetPasswordRequest request = new ResetPasswordRequest("test@test.com");
        String tempPw = "TempPW1234";
        given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

        // when
        authService.resetPassword(request);

        // then
        then(mailService).should(times(0)).sendMail(request.email(), tempPw);
    }

    @DisplayName("유효기간이 만료되지 않았으면 액세스 토큰 재발급에 성공한다")
    @Test
    void reissueAccessTokenShouldSucceed() throws MessagingException {
        // given
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, LocalDateTime.now(), "test@test.com", "test", null, Role.USER, false);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        Date exp = new Date(System.currentTimeMillis() + 2 * 60 * 1000);
        claims.put("exp", exp);
        given(jwtTokenProvider.generateAccessToken(claims, userDto.email())).willReturn("ACCESS_TOKEN");

        // when
        String accessToken = authService.reissueAccessToken(claims, userDto);

        // then
        assertEquals("ACCESS_TOKEN", accessToken);
    }

    @DisplayName("유효기간이 만료되었을 경우 액세스 토큰 재발급에 실패하고 InvalidTokenException을 반환한다.")
    @Test
    void reissueAccessTokenShouldFailWhenInvalidToken() throws MessagingException {
        // given
        UserDto userDto = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test@test.com", "test", null, Role.USER, false);
        Map<String, Object> claims = new HashMap<>();
        Date exp = new Date(System.currentTimeMillis() - 2 * 60 * 1000);
        claims.put("exp", exp);

        // when
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            authService.reissueAccessToken(claims, userDto);
        });

        // then
        assertEquals("검증되지 않은 토큰입니다.", exception.getErrorCode().getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getErrorCode().getStatus());
        assertEquals("access", exception.getDetails().get("type"));
    }

    @DisplayName("유효기간이 만료되지 않았으면 리프레시 토큰 재발급에 성공한다")
    @Test
    void reissueRefreshTokenShouldSucceed() throws MessagingException {
        // given
        UUID userId = UUID.randomUUID();
        UserDto userDto = new UserDto(userId, LocalDateTime.now(), "test@test.com", "test", null, Role.USER, false);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        Date exp = new Date(System.currentTimeMillis() + 2 * 60 * 1000);
        claims.put("exp", exp);
        given(jwtTokenProvider.generateRefreshToken(claims, userDto.email())).willReturn("REFRESH_TOKEN");

        // when
        String refreshToken = authService.reissueRefreshToken(claims, userDto);

        // then
        assertEquals("REFRESH_TOKEN", refreshToken);
    }

    @DisplayName("유효기간이 만료되었을 경우 액세스 토큰 재발급에 실패하고 InvalidTokenException을 반환한다.")
    @Test
    void reissueRefreshTokenShouldFailWhenInvalidToken() throws MessagingException {
        // given
        UserDto userDto = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test@test.com", "test", null, Role.USER, false);
        Map<String, Object> claims = new HashMap<>();
        Date exp = new Date(System.currentTimeMillis() - 2 * 60 * 1000);
        claims.put("exp", exp);

        // when
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            authService.reissueRefreshToken(claims, userDto);
        });

        // then
        assertEquals("검증되지 않은 토큰입니다.", exception.getErrorCode().getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getErrorCode().getStatus());
        assertEquals("refresh", exception.getDetails().get("type"));
    }
}
