package com.codeit.mopl.domain.notification.exception;

import com.codeit.mopl.exception.notification.ErrorCode;

public class NotificationNotAuthentication extends NotificationException {
  public NotificationNotAuthentication() {
    super(ErrorCode.NOTIFICATION_UNAUTHORIZED);
  }
}
