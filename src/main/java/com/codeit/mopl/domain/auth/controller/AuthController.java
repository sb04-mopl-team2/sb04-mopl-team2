package com.codeit.mopl.domain.auth.controller;

import com.codeit.mopl.domain.auth.dto.JwtDto;
import com.codeit.mopl.domain.auth.service.AuthService;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.security.jwt.JwtInformation;
import com.codeit.mopl.security.jwt.JwtRegistry;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRegistry jwtRegistry;
    private final UserService userService;

    @GetMapping("/csrf-token")
    public ResponseEntity getCsrfToken(CsrfToken csrfToken) {
        String tokenValue = csrfToken.getToken();
        log.debug("CSRF 토큰 요청 : {}", tokenValue);
        return ResponseEntity.status(203).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity reissueToken(@CookieValue("REFRESH_TOKEN") String refreshToken, HttpServletResponse response) {
        log.info("AccessToken 재발급 요청");
        Map<String, Object> claims = jwtTokenProvider.getClaims(refreshToken);
        UserDto findUserDto = userService.findByEmail((String) claims.get("sub"));

        if (!jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)) {
            log.warn("RefreshToken이 만료 됨 refreshToken = {}", refreshToken);
            throw new IllegalArgumentException("RefreshToken<UNK> <UNK> <UNK> <UNK> <UNK>");
//            throw new InvalidRefreshTokenException(ErrorCode.INVALID_REFRESH_TOKEN, Map.of("refreshToken", refreshToken));
        }

        String newAccessToken = authService.reissueAccessToken(claims,findUserDto);
        String newRefreshToken = authService.reissueRefreshToken(claims,findUserDto);

        JwtDto responseDto = new JwtDto(findUserDto, newAccessToken);

        Cookie newRefreshTokenCookie = new Cookie("REFRESH_TOKEN", newRefreshToken);
        newRefreshTokenCookie.setMaxAge(jwtTokenProvider.getRefreshTokenExpirationMinutes() * 60);
        newRefreshTokenCookie.setPath("/");
        response.addCookie(newRefreshTokenCookie);

        JwtInformation jwtInformation = new JwtInformation(findUserDto, newAccessToken, newRefreshToken);
        jwtRegistry.rotateJwtInformation(refreshToken, jwtInformation);
        log.debug("AccessToken 재발급 성공 및 Rotate 응답 완료");
        return ResponseEntity.ok(responseDto);
    }
}
