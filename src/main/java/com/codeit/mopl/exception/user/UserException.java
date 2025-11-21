package com.codeit.mopl.exception.user;

import com.codeit.mopl.exception.global.MoplException;

import java.util.Map;

public class UserException extends MoplException {
    public UserException(UserErrorCode userErrorCode, Map<String, Object> details) {
        super(userErrorCode, details);
    }
}
