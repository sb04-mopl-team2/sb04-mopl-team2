package com.codeit.mopl.security.jwt.handler;

import com.codeit.mopl.exception.global.ErrorResponse;
import com.codeit.mopl.exception.user.LoginFailException;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {
    private final ObjectMapper objectMapper;

    public LoginFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.warn("[사용자 관리] 로그인 실패");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        LoginFailException e = new LoginFailException(UserErrorCode.LOGIN_FAIL,null);
        ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode().getName(), e.getErrorCode().getMessage(), e.getDetails(), e.getTimestamp());
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
