package com.codeit.mopl.exception.user;

import java.util.Map;

public class UserLockedException extends UserException {
    public UserLockedException(ErrorCode errorCode, Map<String, Object> details){
        super(errorCode, details);
    }
}
