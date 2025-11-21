package com.codeit.mopl.exception.user;

import java.util.Map;

public class ProfileUploadFailException extends UserException {
    public ProfileUploadFailException(UserErrorCode userErrorCode, Map<String, Object> details) {
        super(userErrorCode, details);
    }
}
