package com.codeit.mopl.exception.watchingsession;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WatchingSessionErrorCode implements ErrorCodeInterface {
  CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다."),
  WATCHING_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "실시간 시청을 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String message;

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
