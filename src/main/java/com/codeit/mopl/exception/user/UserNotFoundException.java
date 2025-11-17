package com.codeit.mopl.exception.user;

import java.util.Map;

public class UserNotFoundException extends UserException{

    public UserNotFoundException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode,details);
    }
}
