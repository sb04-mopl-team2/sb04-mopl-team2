package com.codeit.mopl.exception.notification;

import lombok.Getter;

@Getter
public enum ErrorCode {
  NOTIFICATION_NOT_FOUND("알림을 찾을 수 없습니다.",404),
  NOTIFICATION_UNAUTHORIZED("알림을 수정할 권리가 없습니다.",403);
  private String message;
  private int code;

  ErrorCode(String message, int code) {
    this.message = message;
    this.code = code;
  }
}
