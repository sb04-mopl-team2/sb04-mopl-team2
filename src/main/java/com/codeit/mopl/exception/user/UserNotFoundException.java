package com.codeit.mopl.exception.user;

import java.util.Map;

public class UserNotFoundException extends UserException{

    public UserNotFoundException(UserErrorCode userErrorCode, Map<String, Object> details) {
        super(userErrorCode,details);
    }
}
