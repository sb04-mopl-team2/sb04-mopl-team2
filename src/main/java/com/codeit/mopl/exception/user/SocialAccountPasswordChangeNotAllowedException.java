package com.codeit.mopl.exception.user;

import java.util.Map;

public class SocialAccountPasswordChangeNotAllowedException extends UserException{
    public SocialAccountPasswordChangeNotAllowedException(UserErrorCode userErrorCode, Map<String, Object> details) {
        super(userErrorCode, details);
    }
}
