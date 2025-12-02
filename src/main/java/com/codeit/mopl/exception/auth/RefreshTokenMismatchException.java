package com.codeit.mopl.exception.auth;

import java.util.Map;

public class RefreshTokenMismatchException extends AuthException{
    public RefreshTokenMismatchException(AuthErrorCode authErrorCode, Map<String, Object> details) {
        super(authErrorCode, details);
    }
}
