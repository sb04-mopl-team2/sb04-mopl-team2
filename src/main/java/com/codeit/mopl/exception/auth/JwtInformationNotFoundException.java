package com.codeit.mopl.exception.auth;

import java.util.Map;

public class JwtInformationNotFoundException extends AuthException{
    public JwtInformationNotFoundException(AuthErrorCode authErrorCode, Map<String, Object> details) {
        super(authErrorCode, details);
    }
}
