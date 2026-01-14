package com.codeit.mopl.outbox.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutBoxSortBy {
    CREATED_AT("createdAt"),
    RETRY_COUNT("retryCount");

    private final String value;
}
