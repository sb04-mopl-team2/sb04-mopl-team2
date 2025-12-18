package com.codeit.mopl.exception.user;

import java.util.Map;

public class ProfileDeleteFailException extends UserException {
    public ProfileDeleteFailException(UserErrorCode userErrorCode, Map<String, Object> details) {
        super(userErrorCode, details);
    }
}
