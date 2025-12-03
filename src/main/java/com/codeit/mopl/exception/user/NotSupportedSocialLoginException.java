package com.codeit.mopl.exception.user;

import java.util.Map;

public class NotSupportedSocialLoginException extends UserException{
    public NotSupportedSocialLoginException(UserErrorCode userErrorCode, Map<String, Object> details) {
        super(userErrorCode, details);
    }
}
