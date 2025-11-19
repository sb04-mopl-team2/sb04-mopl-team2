package com.codeit.mopl.exception.review;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import org.springframework.http.HttpStatus;

public enum ErrorCode implements ErrorCodeInterface {
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "리뷰를 수정할 권한이 없습니다."),
    REVIEW_DUPLICATED(HttpStatus.UNAUTHORIZED, "리뷰는 한 사람당 한 개만 작성할 수 있습니다."),;

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
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
