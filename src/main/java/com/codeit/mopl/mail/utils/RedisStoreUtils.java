package com.codeit.mopl.mail.utils;

import com.codeit.mopl.exception.user.TempPasswordStoreFailException;
import com.codeit.mopl.exception.user.UserErrorCode;
import io.lettuce.core.RedisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisStoreUtils {
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    @Value("${mail.expiration:180}")
    private int expiration;
    @Retryable(
            retryFor = {RedisSystemException.class, RedisConnectionFailureException.class, RedisException.class},
            recover = "recover",
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void storeTempPassword(String key, String tempPw){
        String encodedTempPw = passwordEncoder.encode(tempPw);
        redisTemplate.opsForValue().set(key, encodedTempPw, expiration, TimeUnit.SECONDS);
        log.info("[Redis] 임시 비밀번호 설정 완료 key = {}", key);
    }

    @Recover
    public void recover(Exception e, String key, String tempPw) {
        log.warn("[Redis] 임시 비밀번호 저장 실패 key = {}", key);
        throw new TempPasswordStoreFailException(UserErrorCode.TEMP_PASSWORD_STORE_FAIL, Map.of("key", key));
    }
}
