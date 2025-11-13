package com.codeit.mopl.domain.user.controller;

import com.codeit.mopl.domain.user.dto.request.UserCreateRequest;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("유저 생성 호출 email = {}", request.email());
        UserDto response = userService.create(request);
        log.info("유저 생성 응답 userId = {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
