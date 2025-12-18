package com.codeit.mopl.exception.global;

import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
public class MoplException extends RuntimeException {

    final Instant timestamp;
    final ErrorCodeInterface errorCode;
    final Map<String, Object> details;

    public MoplException(ErrorCodeInterface errorCode, Map<String, Object> details) {
        super(errorCode.getName() + ": " + errorCode.getMessage());
        this.timestamp = Instant.now();
        this.errorCode = errorCode;
        this.details = details;
    }
}
