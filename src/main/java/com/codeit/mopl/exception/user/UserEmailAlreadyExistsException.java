package com.codeit.mopl.exception.user;

import java.util.Map;

public class UserEmailAlreadyExistsException extends UserException{
    public UserEmailAlreadyExistsException(UserErrorCode userErrorCode, Map<String, Object> details){
        super(userErrorCode, details);
    }
}
