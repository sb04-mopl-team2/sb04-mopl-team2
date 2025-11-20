package com.codeit.mopl.exception.user;

import java.util.Map;

public class UserLockedException extends UserException {
    public UserLockedException(UserErrorCode userErrorCode, Map<String, Object> details){
        super(userErrorCode, details);
    }
}
