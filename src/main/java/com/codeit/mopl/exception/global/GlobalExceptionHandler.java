package com.codeit.mopl.exception.global;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MoplException.class)
  public ResponseEntity<ErrorResponse> handleMoplException(MoplException e) {
    log.error(e.getMessage(), e);

    ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode().name(), e.getErrorCode().getMessage(), e.getDetails(), e.getTimestamp());

    return ResponseEntity.status(e.getErrorCode().getStatus()).body(errorResponse);
  }

}
