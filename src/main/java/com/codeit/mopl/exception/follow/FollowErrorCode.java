package com.codeit.mopl.exception.follow;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FollowErrorCode implements ErrorCodeInterface {
    FOLLOW_SELF_PROHIBITED(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다."),
    FOLLOW_DUPLICATE(HttpStatus.BAD_REQUEST, "같은 사용자를 중복해서 팔로우할 수 없습니다."),
    FOLLOW_EVENT_MISSING_FOLLOWEE_ID(HttpStatus.BAD_REQUEST, "팔로워 증감 이벤트는 FolloweeId 없이 실행될 수 없습니다.");

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
