package com.codeit.mopl.outbox.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AggregateType {
    FOLLOW("FOLLOW");

    private final String value;

}
