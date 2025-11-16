package com.codeit.mopl.domain.user.controller;

import com.codeit.mopl.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.mopl.domain.user.dto.request.UserCreateRequest;
import com.codeit.mopl.domain.user.dto.request.UserRoleUpdateRequest;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @PatchMapping("/{userId}/password")
    @PreAuthorize("#userId == authentication.principal.user.id")
    public ResponseEntity updatePassword(@PathVariable UUID userId,
                                         @Valid @RequestBody ChangePasswordRequest request) {
        log.info("비밀번호 변경 호출 userId = {}", userId);
        userService.changePassword(userId,request);
        log.info("비밀번호 변경 응답 userId = {}", userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity findUser(@PathVariable UUID userId) {
        log.info("[사용자 관리] 사용자 상세 정보 조회 호출 userId = {}", userId);
        UserDto response = userService.findUser(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity updateRole(@PathVariable UUID userId, @Valid @RequestBody UserRoleUpdateRequest request, @AuthenticationPrincipal UserDetails authenticatedPrincipal) {
        log.info("[사용자 관리] 사용자 권한 변경 호출 userId = {}, adminEmail = {}", userId, authenticatedPrincipal.getUsername());
        userService.updateRole(userId, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
