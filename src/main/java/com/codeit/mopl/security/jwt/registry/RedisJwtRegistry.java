package com.codeit.mopl.security.jwt.registry;

import com.codeit.mopl.event.event.UserLogInOutEvent;
import com.codeit.mopl.exception.auth.AuthErrorCode;
import com.codeit.mopl.exception.auth.JwtInformationNotFoundException;
import com.codeit.mopl.exception.auth.RefreshTokenMismatchException;
import com.codeit.mopl.security.jwt.JwtInformation;
import com.codeit.mopl.security.jwt.provider.JwtTokenProvider;
import com.codeit.mopl.security.jwt.provider.RedisLockProvider;
import com.nimbusds.jose.JOSEException;
import io.lettuce.core.RedisConnectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.RedisConnectionFailureException;
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

        boolean lockAcquired = false;

        try {
            try {
                redisLockProvider.acquireLock(lockKey);
                lockAcquired = true;
            } catch (Exception e) {
                if (isRedisDown(e)) {
                    log.error("[Redis] Redis가 동작 중이 아님, lock 획득 실패 userId={}",
                            jwtInformation.getUserDto().id(), e);
                    return;
                }
                throw e;
            }

            try {
                Object oldestToken = redisTemplate.opsForValue().get(userKey);
                if (oldestToken instanceof JwtInformation oldToken) {
                    removeTokenIndex(oldToken.getAccessToken(), oldToken.getRefreshToken());
                }

                redisTemplate.opsForValue().set(userKey, jwtInformation);
                redisTemplate.expire(userKey, DEFAULT_TTL);
                addTokenIndex(jwtInformation.getAccessToken(), jwtInformation.getRefreshToken());

            } catch (Exception e) {
                if (isRedisDown(e)) {
                    log.error("[Redis] Redis가 동작 중이 아님. userId={}",
                            jwtInformation.getUserDto().id(), e);
                } else {
                    throw e;
                }
            }

        } finally {
            if (lockAcquired) {
                try {
                    redisLockProvider.releaseLock(lockKey);
                } catch (Exception e) {
                    if (isRedisDown(e)) {
                        log.error("[Redis] Redis가 동작 중이 아님, lock 해제 실패 userId={}",
                                jwtInformation.getUserDto().id(), e);
                    } else {
                        throw e;
                    }
                }
            }
            eventPublisher.publishEvent(
                    new UserLogInOutEvent(jwtInformation.getUserDto().id(), true)
            );
        }
    }

    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {
        String userKey = getUserKey(userId);
        String lockKey = userId.toString();

        boolean lockAcquired = false;

        try {
            try {
                redisLockProvider.acquireLock(lockKey);
                lockAcquired = true;
            } catch (Exception e) {
                if (isRedisDown(e)) {
                    log.error("[Redis] Redis가 동작 중이 아님, lock 획득 실패 userId={}", userId, e);
                    return;
                }
                throw e;
            }
            try {
                Object objToken = redisTemplate.opsForValue().get(userKey);
                if (objToken instanceof JwtInformation token) {
                    removeTokenIndex(token.getAccessToken(), token.getRefreshToken());
                }

                redisTemplate.delete(userKey);
            } catch (Exception e) {
                if (isRedisDown(e)) {
                    log.error("[Redis] Redis가 동작중이 아님, 토큰 정리 스킵 userId={}", userId, e);
                } else {
                    throw e;
                }
            }
        } finally {
            if (lockAcquired) {
                try {
                    redisLockProvider.releaseLock(lockKey);
                } catch (Exception e) {
                    if (isRedisDown(e)) {
                        log.error("[Redis] Redis가 동작중이 아님, lock 해제 실패 userId={}", userId, e);
                    } else {
                        throw e;
                    }
                }
            }
            eventPublisher.publishEvent(new UserLogInOutEvent(userId, false));
        }
    }

    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        String userKey = getUserKey(userId);
        try {
            return redisTemplate.opsForValue().get(userKey) != null;
        } catch (Exception e) {
            if (isRedisDown(e)) {
                log.error("[Redis] Redis가 동작 중이 아님, userId={}", userId, e);
                return false;
            }
            log.error("[Redis] Redis 동작 중 예상치 못한 예외, userId={}", userId, e);
            return false;
        }
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.opsForSet().isMember(ACCESS_TOKEN_INDEX_KEY, accessToken)
            );
        } catch (Exception e) {
            if (isRedisDown(e)) {
                log.error("[Redis] Redis가 동작 중이 아님, AccessToken 검증만 실행", e);
                try {
                    return jwtTokenProvider.validateAccessToken(accessToken);
                } catch (JOSEException jse) {
                    return false;
                }
            }
            log.error("[Redis] AccessToken 검증 중 예기치 못한 예외", e);
            return false;
        }
    }

    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.opsForSet().isMember(REFRESH_TOKEN_INDEX_KEY, refreshToken)
            );
        } catch (Exception e) {
            if (isRedisDown(e)) {
                log.error("[Redis] Redis가 동작중이 아님, RefreshToken 검증만 실행", e);
                try {
                    return jwtTokenProvider.validateRefreshToken(refreshToken);
                } catch (JOSEException jse) {
                    return false;
                }
            }
            log.error("[Redis] RefreshToken 검증 중 예기치 못한 예외", e);
            return false;
        }
    }

    @Retryable(retryFor = RedisLockProvider.RedisLockAcquisitionException.class, maxAttempts = 10,
            backoff = @Backoff(delay = 100, multiplier = 2))
    @Override
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        String userKey = getUserKey(newJwtInformation.getUserDto().id());
        String lockKey = newJwtInformation.getUserDto().id().toString();

        boolean lockAcquired = false;

        try {
            try {
                redisLockProvider.acquireLock(lockKey);
                lockAcquired = true;
            } catch (Exception e) {
                if (isRedisDown(e)) {
                    log.error("[Redis] Redis가 동작 중이 아님, lock 획득 실패 userId={}",
                            newJwtInformation.getUserDto().id(), e);
                    return;
                }
                throw e;
            }

            try {
                Object oldToken = redisTemplate.opsForValue().get(userKey);
                if (!(oldToken instanceof JwtInformation jwtInformation)) {
                    throw new JwtInformationNotFoundException(AuthErrorCode.JWT_INFORMATION_NOT_FOUND, Map.of("userKey", userKey));
                }
                if (!jwtInformation.getRefreshToken().equals(refreshToken)) {
                    throw new RefreshTokenMismatchException(AuthErrorCode.REFRESH_TOKEN_MISMATCH, Map.of("userKey", userKey));
                }

                removeTokenIndex(jwtInformation.getAccessToken(), jwtInformation.getRefreshToken());
                jwtInformation.rotate(newJwtInformation.getAccessToken(), newJwtInformation.getRefreshToken());
                redisTemplate.opsForValue().set(userKey, jwtInformation);
                addTokenIndex(newJwtInformation.getAccessToken(), newJwtInformation.getRefreshToken());
                redisTemplate.expire(userKey, DEFAULT_TTL);

            } catch (Exception e) {
                if (isRedisDown(e)) {
                    log.error("[Redis] Redis가 동작 중이 아님, userId={}",
                            newJwtInformation.getUserDto().id(), e);
                    return;
                }
                throw e;
            }

        } finally {
            if (lockAcquired) {
                try {
                    redisLockProvider.releaseLock(lockKey);
                } catch (Exception e) {
                    if (isRedisDown(e)) {
                        log.error("[Redis] Redis가 동작 중이 아님, lock 해제 실패 userId={}",
                                newJwtInformation.getUserDto().id(), e);
                    } else {
                        throw e;
                    }
                }
            }
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
                        boolean isExpired =
                                !jwtTokenProvider.validateAccessToken(jwtInformation.getAccessToken()) ||
                                        !jwtTokenProvider.validateRefreshToken(jwtInformation.getRefreshToken());
                        if (isExpired) {
                            removeTokenIndex(jwtInformation.getAccessToken(), jwtInformation.getRefreshToken());
                            redisTemplate.delete(userKey);
                        }
                    }
                } catch (Exception e) {
                    if (isRedisDown(e)) {
                        log.warn("[Redis] Redis가 동작 중이 아님, key={} 정리 스킵", userKey, e);
                        break;
                    }
                    log.warn("[Redis] Token 정리 중 예외 발생, 정리 스킵 userKey = {}, msg = {}",
                            userKey, e.getMessage());
                }
            }
        } catch (Exception e) {
            if (isRedisDown(e)) {
                log.warn("[Redis] Redis가 동작 중이 아님, 전체 정리 작업 스킵", e);
                return;
            }
            throw e;
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

    private boolean isRedisDown(Throwable e) {
        while (e != null) {
            if (e instanceof RedisConnectionFailureException ||
                    e instanceof RedisConnectionException) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }
}
