package com.codeit.mopl.security.jwt.handler;

import com.codeit.mopl.domain.auth.dto.JwtDto;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.security.CustomUserDetails;
import com.codeit.mopl.security.jwt.JwtInformation;
import com.codeit.mopl.security.jwt.JwtRegistry;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final JwtRegistry jwtRegistry;

    @Value("${jwt.refresh-token-expiration-minutes}")
    private int expiration;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("로그인 성공 username = {}", authentication.getName());

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserDto userDto = userDetails.getUser();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDto.id());
        claims.put("roles", userDto.role());
        String subject = userDto.email();
        String accessToken = jwtTokenProvider.generateAccessToken(claims, subject);
        String refreshToken = jwtTokenProvider.generateRefreshToken(claims,subject);

        JwtInformation jwtInformation = new JwtInformation(userDto, accessToken, refreshToken);
        if (jwtRegistry.hasActiveJwtInformationByUserId(userDto.id())) {
            log.info("중복 로그인 감지 username = {}", userDto.name());
            jwtRegistry.invalidateJwtInformationByUserId(userDto.id());
        }
        jwtRegistry.registerJwtInformation(jwtInformation);
        // accessToken 응답Body
        JwtDto jwtDto = new JwtDto(userDto, accessToken);

        // RefreshToken Cookie
        Cookie refreshTokenCookie = new Cookie("REFRESH_TOKEN", refreshToken);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(expiration * 60);
        response.addCookie(refreshTokenCookie);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), jwtDto);
    }
}
