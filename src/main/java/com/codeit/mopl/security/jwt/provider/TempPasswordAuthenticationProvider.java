package com.codeit.mopl.security.jwt.provider;

import com.codeit.mopl.security.token.TempPasswordAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationProvider;
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

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        TempPasswordAuthenticationToken token = (TempPasswordAuthenticationToken) authentication;

        String username = token.getName();
        String rawPassword = (String) token.getCredentials();
        String encodedTempPw = token.getEncodedTempPassword();

        if (encodedTempPw == null) {
            return null;
        }

        if (!passwordEncoder.matches(rawPassword, encodedTempPw)) {
            throw new TempPasswordBadCredentialsException(
                    "임시 비밀번호가 설정된 계정입니다. 임시 비밀번호를 확인해 주세요."
            );
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        return new TempPasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities(),
                encodedTempPw
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TempPasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public static class TempPasswordBadCredentialsException extends AccountStatusException {
        public TempPasswordBadCredentialsException(String msg) {
            super(msg);
        }
    }
}
