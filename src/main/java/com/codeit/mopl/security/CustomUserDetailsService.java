package com.codeit.mopl.security;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.user.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = findUserByEmail(email);
        UserDto userDto = userMapper.toDto(user);
        String password = user.getPassword();
        return new CustomUserDetails(userDto,password);
    }

    private User findUserByEmail(String email) {
        User user = userRepository.findByEmail((email))
                .orElseThrow(() ->
                        new UsernameNotFoundException(ErrorCode.USER_NOT_FOUND.getMessage()));
        return user;
    }
}
