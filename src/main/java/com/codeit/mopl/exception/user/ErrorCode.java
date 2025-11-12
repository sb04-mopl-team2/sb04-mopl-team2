package com.codeit.mopl.exception.user;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND("유저를 찾을 수 없습니다.",404);
    private String message;
    private int code;

    ErrorCode(String message, int code) {
        this.message = message;
        this.code = code;
    }
}
