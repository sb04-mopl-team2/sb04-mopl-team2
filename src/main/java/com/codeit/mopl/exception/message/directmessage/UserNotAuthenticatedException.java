package com.codeit.mopl.exception.message.directmessage;

import com.codeit.mopl.exception.message.MessageErrorCode;
import com.codeit.mopl.exception.message.MessageException;

import java.util.Map;

public class UserNotAuthenticatedException extends MessageException {
    public UserNotAuthenticatedException(MessageErrorCode errorCode, Map<String, Object> details) {
        super(errorCode,details);
    }
}
