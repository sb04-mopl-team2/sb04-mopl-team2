package com.codeit.mopl.domain.notification.exception;


import com.codeit.mopl.exception.notification.NotificationErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class NotificationException extends RuntimeException {

    private final Instant timestamp;
    private final NotificationErrorCode notificationErrorCode;
    private final Map<String, Object> details;

    public NotificationException(NotificationErrorCode notificationErrorCode) {
        super(notificationErrorCode.getMessage());
        this.timestamp = Instant.now();
        this.notificationErrorCode = notificationErrorCode;
        this.details = new HashMap<>();
    }

    public NotificationException(NotificationErrorCode notificationErrorCode, Throwable cause) {
        super(notificationErrorCode.getMessage(), cause);
        this.timestamp = Instant.now();
        this.notificationErrorCode = notificationErrorCode;
        this.details = new HashMap<>();
    }

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }
}