package com.codeit.mopl.exception.notification;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotificationNotFoundException extends NotificationException {
    public NotificationNotFoundException(NotificationErrorCode notificationErrorCode, Map<String, Object> details) {
        super(notificationErrorCode,details);
    }

} 