package com.codeit.mopl.exception.user;

import java.util.Map;

public class TempPasswordStoreFailException extends UserException {
    public TempPasswordStoreFailException(UserErrorCode userErrorCode, Map<String, Object> details) {
        super(userErrorCode, details);
    }
}
