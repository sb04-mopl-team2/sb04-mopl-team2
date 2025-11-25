package com.codeit.mopl.exception.global;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements ErrorCodeInterface{

  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러가 발생했습니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
