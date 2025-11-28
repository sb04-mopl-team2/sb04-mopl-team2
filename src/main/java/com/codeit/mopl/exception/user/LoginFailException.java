package com.codeit.mopl.exception.user;

import java.util.Map;

public class LoginFailException extends UserException {
    public LoginFailException(UserErrorCode userErrorCode, Map<String, Object> details) {
        super(userErrorCode, details);
    }
}
