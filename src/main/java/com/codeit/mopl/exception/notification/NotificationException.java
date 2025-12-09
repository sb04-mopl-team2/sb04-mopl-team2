package com.codeit.mopl.exception.notification;


import com.codeit.mopl.exception.global.MoplException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class NotificationException extends MoplException {
    public NotificationException(NotificationErrorCode notificationErrorCode, Map<String, Object> details) {
        super(notificationErrorCode, details);
    }
}