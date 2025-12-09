package com.codeit.mopl.exception.notification;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import org.springframework.http.HttpStatus;

public enum NotificationErrorCode implements ErrorCodeInterface {
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
  NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "알림에 대한 권한이 없습니다.");

  private final HttpStatus status;
  private final String message;

  NotificationErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public HttpStatus getStatus() {
    return status;
  }
}
