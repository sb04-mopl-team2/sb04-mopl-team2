package com.codeit.mopl.exception.global;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Getter;

@Getter
public class MoplException extends RuntimeException {

  final LocalDateTime timestamp;
  final ErrorCodeInterface errorCode;
  final Map<String, Object> details;

  public MoplException(ErrorCodeInterface errorCode, Map<String, Object> details) {
    this.timestamp = LocalDateTime.now();
    this.errorCode = errorCode;
    this.details = details;
  }
}
