package com.codeit.mopl.domain.notification.exception;

import com.codeit.mopl.exception.notification.ErrorCode;
import java.util.UUID;

public class NotificationNotFoundException extends NotificationException {
    public NotificationNotFoundException() {
        super(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

} 