package com.codeit.mopl.exception.auth;

import java.util.Map;

public class InvalidTokenException extends AuthException {
    public InvalidTokenException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
