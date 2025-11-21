package com.codeit.mopl.mail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender javaMailSender;

    public void sendMail(String email, String tempPw) throws MessagingException {
        log.info("[이메일] 이메일 전송 요청 email = {}", email);
        String emailContent = getEmailContent(tempPw);

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(email);
        helper.setSubject("임시 비밀번호 발급");
        helper.setText(emailContent, true);

        javaMailSender.send(message);
        log.info("[이메일] 이메일 발송 완료 email = {}", email);
    }

    private String getEmailContent(String tempPw) {
        return "<p> 임시 비밀번호 </p>" +
                "<p><strong>" + tempPw + "</strong></p>";
    }
}
