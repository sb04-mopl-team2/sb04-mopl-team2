package com.codeit.mopl.exception.message;

import com.codeit.mopl.exception.global.MoplException;

import java.util.Map;

public class MessageException extends MoplException {
    public MessageException(MessageErrorCode errorCode, Map<String, Object> details) {
        super(errorCode,details);
    }
}
