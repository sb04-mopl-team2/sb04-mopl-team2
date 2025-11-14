package com.codeit.mopl.domain.auth.service;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;

    public String reissueAccessToken(Map<String, Object> claims, UserDto userDto) {
        log.info("AccessToken 재발급 시도");
        Date expiredAt = (Date) claims.get("exp");
        if (expiredAt.before(new Date())) {
            log.warn("RefreshToken 유효기간이 만료 됨");
            throw new IllegalArgumentException("RefreshToken<UNK> <UNK> <UNK> <UNK> <UNK>");
//            throw new InvalidRefreshTokenException(ErrorCode.INVALID_REFRESH_TOKEN, Map.of("expiredAt", expiredAt));
        }

        return jwtTokenProvider.generateAccessToken(claims, userDto.email());
    }

    public String reissueRefreshToken(Map<String, Object> claims, UserDto userDto) {
        log.info("RefreshToken 재발급 시도");
        Date expiredAt = (Date) claims.get("exp");
        if (expiredAt.before(new Date())) {
            log.warn("RefreshToken 유효기간이 만료 됨");
            throw new IllegalArgumentException("RefreshToken<UNK> <UNK> <UNK> <UNK> <UNK>");
//            throw new InvalidRefreshTokenException(ErrorCode.INVALID_REFRESH_TOKEN, Map.of("expiredAt", expiredAt));
        }

        return jwtTokenProvider.generateRefreshToken(claims, userDto.email());
    }
}
