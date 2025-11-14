package com.codeit.mopl.domain.user.service;

import com.codeit.mopl.domain.user.dto.request.UserCreateRequest;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserDto create(UserCreateRequest request) {
        log.info("유저 생성 실행 email = {}",request.email());
        validateEmail(request.email());
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = new User(request.email(), encodedPassword, request.name());
        userRepository.save(user);
        log.info("유저 생성 완료 userEmail = {}", user.getEmail());
        return userMapper.toDto(user);
    }

    private void validateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("이메일 중복 가입 email = {}", email);
            throw new IllegalArgumentException("Email already exists");
        }
    }
}
