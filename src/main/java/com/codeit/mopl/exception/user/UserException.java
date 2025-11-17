package com.codeit.mopl.exception.user;

import com.codeit.mopl.exception.global.MoplException;

import java.util.Map;

public class UserException extends MoplException {
    public UserException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
