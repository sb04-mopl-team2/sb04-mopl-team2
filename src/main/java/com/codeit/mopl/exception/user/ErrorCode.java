package com.codeit.mopl.exception.user;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

public enum ErrorCode implements ErrorCodeInterface {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    USER_LOCKED(HttpStatus.UNAUTHORIZED, "계정이 잠겨있습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "해당 이메일로 가입된 아이디가 이미 존재합니다."),
    PROFILE_UPLOAD_FAIL(HttpStatus.BAD_REQUEST, "프로필 업로드 실패");

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
