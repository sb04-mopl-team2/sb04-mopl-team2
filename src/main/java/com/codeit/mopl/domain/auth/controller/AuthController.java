package com.codeit.mopl.domain.auth.controller;

import com.codeit.mopl.domain.auth.dto.JwtDto;
import com.codeit.mopl.domain.auth.dto.request.ResetPasswordRequest;
import com.codeit.mopl.domain.auth.service.AuthService;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.exception.auth.ErrorCode;
import com.codeit.mopl.exception.auth.InvalidTokenException;
import com.codeit.mopl.security.jwt.JwtInformation;
import com.codeit.mopl.security.jwt.JwtRegistry;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
        log.debug("[CSRF] CSRF 토큰 요청 = {}", tokenValue);
        return ResponseEntity.status(203).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity reissueToken(@CookieValue("REFRESH_TOKEN") String refreshToken, HttpServletResponse response) {
        log.info("[JWT] AccessToken 재발급 요청");
        Map<String, Object> claims = jwtTokenProvider.getClaims(refreshToken);
        UserDto findUserDto = userService.findByEmail((String) claims.get("sub"));

        if (!jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)) {
            log.warn("[JWT] RefreshToken이 만료 됨 refreshToken = {}", refreshToken);
            throw new InvalidTokenException(ErrorCode.TOKEN_INVALID, Map.of("type", "refresh"));
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
        log.debug("[JWT] AccessToken 재발급 성공 및 Rotate 응답 완료");
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/reset-password")
    public ResponseEntity resetPassword(@Valid @RequestBody ResetPasswordRequest request) throws MessagingException {
        log.info("[사용자] 비밀번호 초기화 요청 email = {}", request.email());

        authService.resetPassword(request);
        log.info("[사용자] 비밀번호 초기화 응답 email = {}", request.email());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
