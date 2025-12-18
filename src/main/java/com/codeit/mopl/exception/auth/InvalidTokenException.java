package com.codeit.mopl.exception.auth;

import java.util.Map;

public class InvalidTokenException extends AuthException {
    public InvalidTokenException(AuthErrorCode authErrorCode, Map<String, Object> details) {
        super(authErrorCode, details);
    }
}
