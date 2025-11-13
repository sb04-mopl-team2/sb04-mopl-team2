package com.codeit.mopl.security;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @DisplayName("로그인에 올바른 이메일을 입력하면 해당 이메일을 가진 유저를 조회하고 정보를 가져온다.")
    @Test
    void loadUserByUsername() {
        // given
        User user = new User("test@example.com", "encodedPassword", "TEST");
        UserDto userDto = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test@example.com", "TEST", null, Role.USER, false);
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(userMapper.toDto(user))
                .thenReturn(userDto);

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        // then
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @DisplayName("로그인에 사용된 이메일이 존재하지 않는 경우 유저를 찾을 수 없다.")
    @Test
    void loadUserByUsernameNotFound() {
        // when & then
        Exception exception = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("test@test.com"));
        assertEquals("유저를 찾을 수 없습니다.",exception.getMessage());
    }

}
