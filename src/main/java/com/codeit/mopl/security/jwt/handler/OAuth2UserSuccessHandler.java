package com.codeit.mopl.security.jwt.handler;

import com.codeit.mopl.domain.auth.dto.JwtDto;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.security.CustomUserDetails;
import com.codeit.mopl.security.jwt.JwtInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class OAuth2UserSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final com.codeit.mopl.security.jwt.provider.JwtTokenProvider jwtTokenProvider;
    private final com.codeit.mopl.security.jwt.registry.JwtRegistry jwtRegistry;

    @Value("${jwt.refresh-token-expiration-minutes}")
    private int expiration;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("Authentication successful");
        log.info("[OAuth2] SuccessHandler 진입: uri={}, Host={}",
                request.getRequestURI(),
                request.getHeader("Host"));

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        UserDto userDto = principal.getUser();
        String email = userDto.email();
        List<String> authorities = List.of(String.valueOf(userDto.role()));
        String accessToken = delegateAccessToken(userDto.id(), email, authorities);
        String refreshToken = delegateRefreshToken(userDto.id(), email, authorities);

        JwtInformation jwtInformation = new JwtInformation(userDto, accessToken, refreshToken);
        if (jwtRegistry.hasActiveJwtInformationByUserId(userDto.id())) {
            log.info("[사용자 관리] 중복 로그인 감지 username = {}", userDto.name());
            jwtRegistry.invalidateJwtInformationByUserId(userDto.id());
        }
        jwtRegistry.registerJwtInformation(jwtInformation);
        // accessToken 응답Body
        JwtDto jwtDto = new JwtDto(userDto, accessToken);

        // RefreshToken Cookie
        ResponseCookie cookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .path("/")
                .maxAge(Duration.ofMinutes(expiration))
                .httpOnly(true)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());


        String redirectUri = "/";
        // 테스트
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost  = request.getHeader("X-Forwarded-Host");
        String host           = request.getHeader("Host");

        String scheme = forwardedProto != null ? forwardedProto : request.getScheme();
        String domain = forwardedHost != null ? forwardedHost : host;

        String absoluteRedirectUrl = scheme + "://" + domain + redirectUri;

        log.info("[OAuth2] 실제 최종 URL = {}", absoluteRedirectUrl);

        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private String delegateAccessToken(UUID userId, String username, List<String> authorities) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("roles", authorities);

        String subject = username;

        return jwtTokenProvider.generateAccessToken(claims, subject);
    }

    private String delegateRefreshToken(UUID userId, String username, List<String> authorities) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("roles", authorities);

        String subject = username;

        return jwtTokenProvider.generateRefreshToken(claims, subject);
    }
}
