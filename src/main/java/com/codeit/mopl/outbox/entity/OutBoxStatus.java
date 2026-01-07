package com.codeit.mopl.outbox.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OutBoxStatus {
    REQUESTED("요청됨"),
    PUBLISHED("발행됨"),
    FAILED("실패"),
    DEAD("재시도 횟수 초과");

    private final String value;
}
