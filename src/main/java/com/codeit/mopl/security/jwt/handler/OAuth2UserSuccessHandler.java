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
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Value("${jwt.refresh-token-expiration-minutes}")
    private int expiration;
    @Value("${server.host:localhost}")
    private String host;
    @Value("${server.port:8080}")
    private int port;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        UserDto userDto = principal.getUser();
        String email = userDto.email();
        List<String> authorities = List.of(String.valueOf(userDto.role()));
        String accessToken = delegateAccessToken(userDto.id(), email, authorities);  // (6-1)
        String refreshToken = delegateRefreshToken(userDto.id(), email, authorities);     // (6-2)

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

        String redirectUri = createURI().toString();

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

    private URI createURI() {
        return UriComponentsBuilder
                .newInstance()
                .scheme("http")
                .host(host)
                .port(port)
                .build()
                .toUri();
    }
}
