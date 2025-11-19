package com.codeit.mopl.domain.user.controller;

import com.codeit.mopl.domain.user.dto.request.*;
import com.codeit.mopl.domain.user.dto.response.CursorResponseUserDto;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.ImageContentType;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.exception.user.NotImageContentException;
import com.codeit.mopl.exception.user.UserErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("[사용자 관리] 유저 생성 호출 email = {}", request.email());
        UserDto response = userService.create(request);
        log.info("[사용자 관리] 유저 생성 응답 userId = {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{userId}/password")
    @PreAuthorize("#userId == authentication.principal.user.id")
    public ResponseEntity updatePassword(@PathVariable UUID userId,
                                         @Valid @RequestBody ChangePasswordRequest request) {
        log.info("[사용자 관리] 비밀번호 변경 호출 userId = {}", userId);
        userService.changePassword(userId,request);
        log.info("[사용자 관리] 비밀번호 변경 응답 userId = {}", userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity findUser(@PathVariable UUID userId) {
        log.info("[사용자 관리] 사용자 상세 정보 조회 호출 userId = {}", userId);
        UserDto response = userService.findUser(userId);
        log.info("[사용자 관리] 사용자 상세 정보 조회 응답 userId = {}", response.id());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity updateRole(@PathVariable UUID userId, @Valid @RequestBody UserRoleUpdateRequest request, @AuthenticationPrincipal UserDetails authenticatedPrincipal) {
        log.info("[사용자 관리] 사용자 권한 변경 호출 userId = {}, adminEmail = {}", userId, authenticatedPrincipal.getUsername());
        userService.updateRole(userId, request);
        log.info("[사용자 관리] 사용자 권한 변경 응답 userId = {}, 변경 권한 = {}", userId, request.role());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/{userId}/locked")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity updateLcok(@PathVariable UUID userId, @Valid @RequestBody UserLockUpdateRequest request, @AuthenticationPrincipal UserDetails authenticatedPrincipal) {
        log.info("[사용자 관리] 사용자 계정 잠금 변경 호출 userId = {}, adminEmail = {}", userId, authenticatedPrincipal.getUsername());
        userService.updateLock(userId, request);
        log.info("[사용자 관리] 사용자 계정 잠금 변경 응답 userId = {}, 잠금 상태 = {}", userId, request.locked());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity getAllUsers(@Valid @ModelAttribute CursorRequestUserDto request) {
        log.info("[사용자 관리] 유저 목록 조회 호출");
        CursorResponseUserDto response = userService.getAllUsers(request);
        log.info("[사용자 관리] 유저 목록 조회 응답 totalCount = {}", response.totalCount());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(value = "/{userId}",consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("#userId == authentication.principal.user.id")
    public ResponseEntity updateProfile(@PathVariable UUID userId,
                                        @RequestPart(value = "request") UserUpdateRequest request,
                                        @RequestPart(value = "image", required = false) MultipartFile profileImage) {
        validateImage(profileImage);
        log.info("[사용자 관리] 유저 프로필 변경 호출 userId = {}", userId);
        UserDto response = userService.updateProfile(userId, request, profileImage);
        log.info("[사용자 관리] 유저 프로필 변경 응답 userId = {}", response.id());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    private void validateImage(MultipartFile profileImage) {
        if (profileImage == null) {
            throw new NotImageContentException(UserErrorCode.NOT_IMAGE, Map.of("contentType", "null"));
        }
        if (!ImageContentType.isImage(profileImage.getContentType())) {
            throw new NotImageContentException(UserErrorCode.NOT_IMAGE, Map.of("contentType",profileImage.getContentType()));
        }
    }
}
