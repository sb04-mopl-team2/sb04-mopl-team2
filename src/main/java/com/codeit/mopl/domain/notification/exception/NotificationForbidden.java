package com.codeit.mopl.domain.notification.exception;

import com.codeit.mopl.exception.notification.NotificationErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotificationForbidden extends NotificationException {
  public NotificationForbidden() {
    super(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
  }
}
