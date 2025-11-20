package com.codeit.mopl.exception.auth;

import com.codeit.mopl.exception.global.MoplException;

import java.util.Map;

public class AuthException extends MoplException {
    public AuthException(AuthErrorCode authErrorCode, Map<String, Object> details) {
        super(authErrorCode, details);
    }

}
