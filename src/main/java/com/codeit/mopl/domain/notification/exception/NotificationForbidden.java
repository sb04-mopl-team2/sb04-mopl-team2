package com.codeit.mopl.domain.notification.exception;

import com.codeit.mopl.exception.notification.ErrorCode;

public class NotificationForbidden extends NotificationException {
  public NotificationForbidden() {
    super(ErrorCode.NOTIFICATION_FORBIDDEN);
  }
}
