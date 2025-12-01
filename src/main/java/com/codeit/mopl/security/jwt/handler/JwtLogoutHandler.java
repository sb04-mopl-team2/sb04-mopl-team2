package com.codeit.mopl.security.jwt.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals("REFRESH_TOKEN"))
                .findFirst()
                .ifPresent(cookie -> {
                    cookie.setMaxAge(0); // 즉시 만료
                    cookie.setPath("/");
                    response.addCookie(cookie);
                });
    }
}
