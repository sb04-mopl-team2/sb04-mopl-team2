package com.codeit.mopl.exception.auth;

import com.codeit.mopl.exception.global.MoplException;

import java.util.Map;

public class AuthException extends MoplException {
    public AuthException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }

}
