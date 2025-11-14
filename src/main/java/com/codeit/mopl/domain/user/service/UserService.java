package com.codeit.mopl.domain.user.service;

import com.codeit.mopl.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.mopl.domain.user.dto.request.UserCreateRequest;
import com.codeit.mopl.domain.user.dto.request.UserRoleUpdateRequest;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.user.ErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto create(UserCreateRequest request) {
        log.info("유저 생성 실행 email = {}",request.email());
        validateEmail(request.email());
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = new User(request.email(), encodedPassword, request.name());
        userRepository.save(user);
        log.info("유저 생성 완료 userEmail = {}", user.getEmail());
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto findByEmail(String email) {
        User findUser = findUserByEmail(email);
        UserDto userDto = userMapper.toDto(findUser);
        return userDto;
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("유저 비밀번호 변경 동작 userId = {}", userId);
        User findUser = getValidUserByUserId(userId);
        String encodedNewPassword = passwordEncoder.encode(request.password());
        findUser.updatePassword(encodedNewPassword);
        log.info("유저 비밀번호 변경 완료 userId = {}", userId);
    }

    public UserDto findUser(UUID userId) {
        log.info("[사용자 관리] 회원 정보 조회 동작 userId = {}", userId);
        User findUser = getValidUserByUserId(userId);
        UserDto userDto = userMapper.toDto(findUser);
        log.info("[사용자 관리] 회원 정보 조회 성공 userId = {}", userDto.id());
        return userDto;
    }

    @Transactional
    public void updateRole(UUID userId, UserRoleUpdateRequest request) {
        log.info("[사용자 관리] 회원 권한 수정 동작 userId = {}", userId);
        User findUser = getValidUserByUserId(userId);
        log.debug("[사용자 관리] 회원 권한 수정 {} -> {}", findUser.getRole(), request.role());
        findUser.updateRole(request.role());
        log.info("[사용자 관리] 회원 권한 수정 완료 userId = {}, Role = {}", userId, request.role());
    }

    private void validateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("이메일 중복 가입 email = {}", email);
            throw new IllegalArgumentException("Email already exists");
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.info("해당 유저를 찾을 수 없음 email = {}", email);
                    throw new UsernameNotFoundException("Email not found");
                });
    }

    private User getValidUserByUserId(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[사용자 관리] 해당 유저를 찾을 수 없음 userId = {}", userId);
                    throw new UserNotFoundException(ErrorCode.USER_NOT_FOUND, Map.of("userId",userId));
                });
    }
}
