package com.codeit.mopl.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TempPasswordAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String rawPassword = (String) authentication.getCredentials();

        String encodedTempPw = null;

        try {
            encodedTempPw = redisTemplate.opsForValue().get(username);
        } catch (Exception e) {
            log.error("[Redis] Redis가 정상적으로 동작하지 않고 있습니다.");
            return null;
        }

        if (encodedTempPw == null) {
            return null;
        }

        if (!passwordEncoder.matches(rawPassword, encodedTempPw)) {
            throw new TempPasswordBadCredentialsException("임시 비밀번호가 설정된 계정입니다. 임시 비밀번호를 확인해 주세요.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        return token;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public class TempPasswordBadCredentialsException extends AccountStatusException {
        public TempPasswordBadCredentialsException(String msg) {
            super(msg);
        }
    }
}
