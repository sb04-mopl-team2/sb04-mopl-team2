package com.codeit.mopl.exception.user;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCodeInterface {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    USER_LOCKED(HttpStatus.UNAUTHORIZED, "계정이 잠겨있습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "해당 이메일로 가입된 아이디가 이미 존재합니다."),
    PROFILE_UPLOAD_FAIL(HttpStatus.BAD_REQUEST, "프로필 업로드 실패"),
    PROFILE_DELETE_FAIL(HttpStatus.BAD_REQUEST, "프로필 삭제 실패"),
    NOT_IMAGE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "프로필에 사용된 데이터가 이미지가 아닙니다."),
    USER_ID_IS_NULL(HttpStatus.BAD_REQUEST, "유효한 userId가 아닙니다."),
    TEMP_PASSWORD_STORE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "임시 비밀번호 저장에 실패했습니다."),
    LOGIN_FAIL(HttpStatus.UNAUTHORIZED, "로그인에 실패했습니다."),
    MAIL_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "메일 전송에 실패했습니다."),;

    private final HttpStatus status;
    private final String message;

    UserErrorCode(HttpStatus status, String message) {
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
