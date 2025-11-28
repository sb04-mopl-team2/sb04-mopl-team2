package com.codeit.mopl.exception.batch;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import org.springframework.http.HttpStatus;

public enum BatchErrorCode implements ErrorCodeInterface {
  INITIAL_DATA_LOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "초기 데이터 수집 중 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String message;

  BatchErrorCode(HttpStatus status, String message) {
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