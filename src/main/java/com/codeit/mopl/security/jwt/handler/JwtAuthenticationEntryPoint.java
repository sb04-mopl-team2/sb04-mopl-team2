package com.codeit.mopl.security.jwt.handler;

import com.codeit.mopl.exception.auth.ErrorCode;
import com.codeit.mopl.exception.auth.InvalidTokenException;
import com.codeit.mopl.exception.global.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        InvalidTokenException e = new InvalidTokenException(ErrorCode.TOKEN_INVALID, null);
        ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode().getName(), e.getErrorCode().getMessage(), e.getDetails(), e.getTimestamp());
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
