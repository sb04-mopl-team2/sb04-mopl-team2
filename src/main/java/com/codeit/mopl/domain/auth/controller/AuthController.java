package com.codeit.mopl.domain.auth.controller;

import com.codeit.mopl.domain.auth.dto.request.SignInRequest;
import com.codeit.mopl.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @GetMapping("/csrf-token")
    public ResponseEntity getCsrfToken(CsrfToken csrfToken) {
        String tokenValue = csrfToken.getToken();
        log.debug("CSRF 토큰 요청 : {}", tokenValue);
        return ResponseEntity.status(203).build();
    }
}
