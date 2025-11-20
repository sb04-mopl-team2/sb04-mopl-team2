package com.codeit.mopl.domain.auth.service;

import com.codeit.mopl.domain.auth.dto.request.ResetPasswordRequest;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.user.UserNotFoundException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

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
    private PasswordEncoder passwordEncoder;
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
        String encodedPw = "encodedPw";
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(findUser));
        given(passwordUtils.makeTempPassword()).willReturn(tempPw);
        given(passwordEncoder.encode(tempPw)).willReturn(encodedPw);
        given(userRepository.save(findUser)).willReturn(findUser);
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

}
