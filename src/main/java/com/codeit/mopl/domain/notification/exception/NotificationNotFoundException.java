package com.codeit.mopl.domain.notification.exception;

import com.codeit.mopl.exception.notification.NotificationErrorCode;

public class NotificationNotFoundException extends NotificationException {
    public NotificationNotFoundException() {
        super(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

} 