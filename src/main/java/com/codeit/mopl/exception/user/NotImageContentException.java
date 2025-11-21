package com.codeit.mopl.exception.user;

import java.util.Map;

public class NotImageContentException extends UserException{
    public NotImageContentException(UserErrorCode userErrorCode, Map<String, Object> details) {
        super(userErrorCode, details);
    }
}
