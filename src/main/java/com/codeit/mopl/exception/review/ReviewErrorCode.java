package com.codeit.mopl.exception.review;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import org.springframework.http.HttpStatus;

public enum ReviewErrorCode implements ErrorCodeInterface {
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "리뷰에 대한 권한이 없습니다."),
    REVIEW_DUPLICATED(HttpStatus.FORBIDDEN, "리뷰는 한 사람당 한 개만 작성할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

    ReviewErrorCode(HttpStatus status, String message) {
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
