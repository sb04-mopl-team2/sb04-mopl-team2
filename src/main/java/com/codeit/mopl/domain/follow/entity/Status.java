package com.codeit.mopl.domain.follow.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Status {
    PENDING("처리중"),
    CANCELLED("처리 중단"),
    CONFIRM("처리 완료"),
    FAILED("처리 실패");

    private final String value;
}
