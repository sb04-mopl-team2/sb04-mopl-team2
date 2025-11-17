package com.codeit.mopl.exception.user;

import java.util.Map;

public class UserEmailAlreadyExistsException extends UserException{
    public UserEmailAlreadyExistsException(ErrorCode errorCode, Map<String, Object> details){
        super(errorCode, details);
    }
}
