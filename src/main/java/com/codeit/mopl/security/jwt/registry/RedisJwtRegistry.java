package com.codeit.mopl.security.jwt.registry;

import com.codeit.mopl.event.event.UserLogInOutEvent;
import com.codeit.mopl.exception.auth.AuthErrorCode;
import com.codeit.mopl.exception.auth.InvalidTokenException;
import com.codeit.mopl.exception.auth.JwtInformationNotFoundException;
import com.codeit.mopl.exception.auth.RefreshTokenMismatchException;
import com.codeit.mopl.security.jwt.JwtInformation;
import com.codeit.mopl.security.jwt.provider.JwtTokenProvider;
import com.codeit.mopl.security.jwt.provider.RedisLockProvider;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisJwtRegistry implements JwtRegistry {

    private static final String USER_JWT_KEY_PREFIX = "jwt:user:";
    private static final String ACCESS_TOKEN_INDEX_KEY = "jwt:access_tokens";
    private static final String REFRESH_TOKEN_INDEX_KEY = "jwt:refresh_tokens";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisLockProvider redisLockProvider;

    @Retryable(retryFor = RedisLockProvider.RedisLockAcquisitionException.class,
            maxAttempts = 10,
            backoff = @Backoff(delay = 100, multiplier = 2))
    @Override
    public void registerJwtInformation(JwtInformation jwtInformation) {
        String userKey = getUserKey(jwtInformation.getUserDto().id());
        String lockKey = jwtInformation.getUserDto().id().toString();

        redisLockProvider.acquireLock(lockKey);
        try {
            Object oldestToken = redisTemplate.opsForValue().get(userKey);
            if (oldestToken instanceof JwtInformation oldToken) {
                removeTokenIndex(oldToken.getAccessToken(), oldToken.getRefreshToken());
            }

            redisTemplate.opsForValue().set(userKey, jwtInformation);
            redisTemplate.expire(userKey, DEFAULT_TTL);
            addTokenIndex(jwtInformation.getAccessToken(), jwtInformation.getRefreshToken());

        } finally {
            redisLockProvider.releaseLock(lockKey);
        }

        eventPublisher.publishEvent(
                new UserLogInOutEvent(jwtInformation.getUserDto().id(), true)
        );
    }

    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {
        String userKey = getUserKey(userId);
        String lockKey = userId.toString();

        redisLockProvider.acquireLock(lockKey);
        try {
            Object objToken = redisTemplate.opsForValue().get(userKey);
            if (objToken instanceof JwtInformation token) {
                removeTokenIndex(token.getAccessToken(), token.getRefreshToken());
            }

            redisTemplate.delete(userKey);
        } finally {
            redisLockProvider.releaseLock(lockKey);
        }
        eventPublisher.publishEvent(new UserLogInOutEvent(userId, false));
    }

    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        String userKey = getUserKey(userId);
        return redisTemplate.opsForValue().get(userKey) != null;
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(ACCESS_TOKEN_INDEX_KEY, accessToken)
        );
    }

    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(REFRESH_TOKEN_INDEX_KEY, refreshToken)
        );
    }

    @Retryable(retryFor = RedisLockProvider.RedisLockAcquisitionException.class, maxAttempts = 10,
            backoff = @Backoff(delay = 100, multiplier = 2))
    @Override
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        String userKey = getUserKey(newJwtInformation.getUserDto().id());
        String lockKey = newJwtInformation.getUserDto().id().toString();

        redisLockProvider.acquireLock(lockKey);
        try {
            Object oldToken = redisTemplate.opsForValue().get(userKey);
            if(!(oldToken instanceof JwtInformation jwtInformation)) {
                throw new JwtInformationNotFoundException(AuthErrorCode.JWT_INFORMATION_NOT_FOUND, Map.of("userKey",userKey));
            }
            if (jwtInformation.getRefreshToken().equals(refreshToken)) {
                throw new RefreshTokenMismatchException(AuthErrorCode.REFRESH_TOKEN_MISMATCH, Map.of("userKey",userKey));
            }
            removeTokenIndex(jwtInformation.getAccessToken(), jwtInformation.getRefreshToken());
            jwtInformation.rotate(newJwtInformation.getAccessToken(), newJwtInformation.getRefreshToken());
            redisTemplate.opsForValue().set(userKey, jwtInformation);
            addTokenIndex(newJwtInformation.getAccessToken(), newJwtInformation.getRefreshToken());
            redisTemplate.expire(userKey, DEFAULT_TTL);
        } finally {
            redisLockProvider.releaseLock(lockKey);
        }
    }

    @Scheduled(fixedDelay = 1000 * 60 * 5)
    @Override
    public void clearExpiredJwtInformation() throws JOSEException {
        ScanOptions options = ScanOptions.scanOptions()
                .match(USER_JWT_KEY_PREFIX + "*")
                .count(100)
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String userKey = cursor.next();
                try {
                    Object token = redisTemplate.opsForValue().get(userKey);
                    if (token instanceof JwtInformation jwtInformation) {
                        boolean isExpired = !jwtTokenProvider.validateAccessToken(jwtInformation.getAccessToken()) ||
                                !jwtTokenProvider.validateRefreshToken(jwtInformation.getRefreshToken());
                        if (isExpired) {
                            removeTokenIndex(jwtInformation.getAccessToken(), jwtInformation.getRefreshToken());
                            redisTemplate.delete(userKey);
                        }
                    }
                } catch (Exception e) {
                    log.warn("[Redis] token 정리 중 예외 발생, 정리스킵 userKey = {}, msg = {}", userKey, e.getMessage());
                }
            }
        }
    }

    private String getUserKey(UUID userId) {
        return USER_JWT_KEY_PREFIX + userId.toString();
    }

    private void addTokenIndex(String accessToken, String refreshToken) {
        redisTemplate.opsForSet().add(ACCESS_TOKEN_INDEX_KEY, accessToken);
        redisTemplate.opsForSet().add(REFRESH_TOKEN_INDEX_KEY, refreshToken);
    }

    private void removeTokenIndex(String accessToken, String refreshToken) {
        redisTemplate.opsForSet().remove(ACCESS_TOKEN_INDEX_KEY, accessToken);
        redisTemplate.opsForSet().remove(REFRESH_TOKEN_INDEX_KEY, refreshToken);
    }
}
