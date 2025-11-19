package com.codeit.mopl.exception.auth;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ErrorCode implements ErrorCodeInterface {
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "검증되지 않은 토큰입니다.");

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
