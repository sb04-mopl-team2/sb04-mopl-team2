package com.codeit.mopl.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
public class JwtRegistry {
    private final JwtTokenProvider jwtTokenProvider;

    private final Map<UUID, Queue<JwtInformation>> origin = new ConcurrentHashMap<>();
    private final int maxActiveJwtCount = 1;

    public JwtRegistry(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void registerJwtInformation(JwtInformation jwtInformation) {
        log.info("JwtInformation 등록 userEmail = {}", jwtInformation.getUserDto().email());
        if (!origin.containsKey(jwtInformation.getUserDto().id())) {
            origin.put(jwtInformation.getUserDto().id(), new ConcurrentLinkedQueue<>());
        }
        Queue<JwtInformation> queue = origin.get(jwtInformation.getUserDto().id());
        if (queue.size() >= maxActiveJwtCount) {
            queue.poll();
        }
        queue.offer(jwtInformation);
    }

    public void invalidateJwtInformationByUserId(UUID userId) {
        log.debug("JwtInformation 삭제 userId = {}", userId);
        origin.remove(userId);
    }

    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        log.debug("JwtInformation 등록 유저 검증 userId = {}", userId);
        Queue<JwtInformation> queue = origin.get(userId);
        if (queue == null || queue.isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        log.debug("JwtInformation AccessToken 검증 accessToken = {}", accessToken);
        return origin.values()
                .stream()
                .flatMap(Collection::stream)
                .anyMatch(jwtInformation -> jwtInformation.getAccessToken().equals(accessToken));
    }

    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        log.debug("JwtInformation Refresh Token 검증 refreshToken = {}", refreshToken);
        return origin.values()
                .stream()
                .flatMap(Collection::stream)
                .anyMatch(jwtInformation -> jwtInformation.getRefreshToken().equals(refreshToken));
    }

    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        log.info("JwtInformation RefreshToken Rotate userEmail = {}", newJwtInformation.getUserDto().email());
        UUID userId = origin.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(jwtInfo ->
                        jwtInfo.getRefreshToken().equals(refreshToken))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("JwtInformation 내 찾을 수 없음 refreshToken = {}", refreshToken);
                    throw new IllegalArgumentException("Invalid refresh token");
                })
                .getUserDto().id();
        Queue<JwtInformation> queue = origin.get(userId);
        if (queue.size() >= maxActiveJwtCount) {
            queue.poll();
        }
        queue.offer(newJwtInformation);
    }

    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void clearExpiredJwtInformation() {
        log.info("유효기간이 만료된 JwtInformation 정리");
        origin.forEach((userId, queue) -> {
            queue.removeIf(jwtInformation ->
                    jwtTokenProvider.isExpired(jwtInformation.getRefreshToken()));
            if (queue.isEmpty()) {
                origin.remove(userId);
            }
        });
    }
}
