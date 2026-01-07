package com.codeit.mopl.domain.follow.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FollowStatus {
    REQUESTED("요청됨"),
    CANCELLED("취소"),
    CONFIRM("처리 완료");

    private final String value;
}
