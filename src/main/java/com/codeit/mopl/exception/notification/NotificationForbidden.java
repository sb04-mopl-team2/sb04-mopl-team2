package com.codeit.mopl.exception.notification;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotificationForbidden extends NotificationException {
  public NotificationForbidden(NotificationErrorCode notificationErrorCode, Map<String, Object> details) {
    super(notificationErrorCode,details);
  }
}
