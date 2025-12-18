package com.codeit.mopl.exception.global;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MoplException.class)
    public ResponseEntity<ErrorResponse> handleMoplException(MoplException e) {
        log.error(e.getMessage(), e);

        ErrorResponse errorResponse = new ErrorResponse(e.getErrorCode().getName(),
                e.getErrorCode().getMessage(), e.getDetails(), e.getTimestamp());

        return ResponseEntity.status(e.getErrorCode().getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ErrorResponse> handleMessagingException(MessagingException e) {
        log.error(e.getMessage(), e);

        ErrorResponse errorResponse = new ErrorResponse("MessagingException", e.getMessage(),
                Map.of("log", "이메일 발신 중 오류 발생"), Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);

        ErrorCodeInterface errorCode = ErrorCode.INVALID_INPUT_VALUE;

        Map<String, Object> errors = new java.util.HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = new ErrorResponse(errorCode.getName(), errorCode.getMessage(), errors, Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

}
