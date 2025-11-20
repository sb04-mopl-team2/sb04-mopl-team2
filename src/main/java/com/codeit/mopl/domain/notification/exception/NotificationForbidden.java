package com.codeit.mopl.domain.notification.exception;

import com.codeit.mopl.exception.notification.NotificationErrorCode;

public class NotificationForbidden extends NotificationException {
  public NotificationForbidden() {
    super(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
  }
}
