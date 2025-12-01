package com.codeit.mopl.exception.notification;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotificationNotFoundException extends NotificationException {
    public NotificationNotFoundException() {
        super(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

} 