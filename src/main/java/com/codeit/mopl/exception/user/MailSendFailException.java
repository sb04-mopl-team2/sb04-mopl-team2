package com.codeit.mopl.exception.user;

import java.util.Map;

public class MailSendFailException extends UserException {
    public MailSendFailException(UserErrorCode userErrorCode, Map<String, Object> details) {
        super(userErrorCode, details);
    }
}
