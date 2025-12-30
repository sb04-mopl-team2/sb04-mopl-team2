package com.codeit.mopl.security.token;

import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
public class TempPasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private final String encodedTempPassword;

    public TempPasswordAuthenticationToken(Object principal, Object credentials, String encodedTempPassword) {
        super(principal, credentials);
        this.encodedTempPassword = encodedTempPassword;
    }

    public TempPasswordAuthenticationToken(Object principal, Object credentials,
                                           Collection<? extends GrantedAuthority> authorities,
                                           String encodedTempPassword) {
        super(principal, credentials, authorities);
        this.encodedTempPassword = encodedTempPassword;
    }

}
