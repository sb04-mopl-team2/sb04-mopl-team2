package com.codeit.mopl.exception.user;

import java.util.Map;

public class ProfileUploadFailException extends UserException {
    public ProfileUploadFailException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
