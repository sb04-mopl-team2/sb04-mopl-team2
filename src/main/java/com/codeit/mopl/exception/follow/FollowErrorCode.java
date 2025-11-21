package com.codeit.mopl.exception.follow;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FollowErrorCode implements ErrorCodeInterface {
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "팔로우를 찾을 수 없습니다."),
    FOLLOW_SELF_PROHIBITED(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다."),
    FOLLOW_DUPLICATE(HttpStatus.BAD_REQUEST, "같은 사용자를 중복해서 팔로우할 수 없습니다."),
    FOLLOW_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 팔로우를 지울 권한이 없습니다."),
    FOLLOWEE_ID_IS_NULL(HttpStatus.BAD_REQUEST, "FolloweeId는 필수 값입니다.");

    private final HttpStatus status;
    private final String message;

    FollowErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getName() {
        return this.name();
    }
}
