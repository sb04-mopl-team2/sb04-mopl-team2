package com.codeit.mopl.domain.auth.controller;

import com.codeit.mopl.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
}
