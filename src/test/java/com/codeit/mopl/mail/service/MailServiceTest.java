package com.codeit.mopl.mail.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MailServiceTest {
    @Mock
    JavaMailSender javaMailSender;

    @InjectMocks
    MailService mailService;

    @Test
    void sendMail_buildsMimeMessage() throws Exception {
        // given
        String email = "test@test.com";
        String tempPw = "Abcd1234!";

        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);

        // when
        mailService.sendMail(email, tempPw);

        // then
        verify(javaMailSender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("임시 비밀번호 발급");
    }
}
