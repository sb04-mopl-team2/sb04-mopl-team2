package com.codeit.mopl.exception.auth;

import com.codeit.mopl.exception.user.UserErrorCode;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

public class OAuth2LockedException
        extends OAuth2AuthenticationException {

    private final UserErrorCode errorCode;
    private final Map<String, Object> details;

    public OAuth2LockedException(UserErrorCode errorCode,
                                                Map<String, Object> details) {
        super(new OAuth2Error(errorCode.getName(), errorCode.getMessage(), null));
        this.errorCode = errorCode;
        this.details = details;
    }
}