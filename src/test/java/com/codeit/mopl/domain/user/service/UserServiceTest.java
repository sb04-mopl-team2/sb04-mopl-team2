package com.codeit.mopl.domain.user.service;

import com.codeit.mopl.domain.user.dto.request.UserCreateRequest;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @DisplayName("이메일, 비밀번호, 이름을 정확히 입력하였고 가입되지 않은 이메일로 회원가입 시 회원가입이 정상적으로 완료한다.")
    @Test
    void createUserShouldSucceedWhenValidateRequest() {
        // given
        UserCreateRequest request = new UserCreateRequest("test", "test@example.com", "testPassword");
        User user = new User("test@example.com","encodedPassword","test");
        UserDto userDto = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test@example.com","test",null, Role.USER, false);
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode("testPassword")).willReturn("encodedPassword");
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toDto(user)).willReturn(userDto);

        // when
        UserDto createdUserDto = userService.create(request);

        // then
        assertEquals(createdUserDto.email(), "test@example.com");
        assertEquals(createdUserDto.role(), Role.USER);
    }

    @DisplayName("이메일, 비밀번호, 이름을 정확히 입력하였지만 이미 가입된 이메일로 회원가입을 시도하는 경우 회원가입에 실패한다.")
    @Test
    void createUserShouldFailWhenSingUpTryExistsEmail() {
        // given
        UserCreateRequest request = new UserCreateRequest("test", "test@example.com", "testPassword");
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> userService.create(request));

        assertEquals("Email already exists", exception.getMessage());
    }
}
