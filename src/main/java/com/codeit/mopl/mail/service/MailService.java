package com.codeit.mopl.mail.service;

import com.codeit.mopl.exception.user.MailSendFailException;
import com.codeit.mopl.exception.user.TempPasswordStoreFailException;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.mail.utils.RedisStoreUtils;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender javaMailSender;

    @Retryable(
            retryFor = { MailException.class, MessagingException.class },
            recover = "recover",
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
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

    @Recover
    public void recover(Exception e,String email, String tempPw) {
        log.warn("[이메일] 이메일 전송 실패 email = {}", email);
        throw new MailSendFailException(UserErrorCode.MAIL_SEND_FAIL,Map.of("email",email));
    }
}
