package com.codeit.mopl.mail.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MailServiceTest {
    @Mock
    JavaMailSender javaMailSender;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOps;

    @InjectMocks
    MailService mailService;

    @DisplayName("이메일 전송에 성공한다")
    @Test
    void sendMailShouldSucceed() throws Exception {
        // given
        String email = "test@test.com";
        String tempPw = "asdf1234!";
        String encodedPw = "encodedPassword";

        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);
        given(passwordEncoder.encode(tempPw)).willReturn(encodedPw);
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        // when
        mailService.sendMail(email, tempPw);

        // then
        verify(javaMailSender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("임시 비밀번호 발급");
    }
}
