package com.codeit.mopl.exception.user;

import java.util.Map;

public class UserIdIsNullException extends UserException {
    public UserIdIsNullException(UserErrorCode userErrorCode, Map<String, Object> details) {
      super(userErrorCode, details);
    }
}
