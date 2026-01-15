package com.codeit.mopl.outbox.dto;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.AggregateType;

import java.time.Instant;
import java.util.Set;

public record DeadOutBoxEventsRetryDto(
        int totalCount,
        Set<EventType> eventTypes,
        Set<AggregateType> aggregateTypes,
        Instant createdFrom,
        Instant createdTo,
        Instant executedAt
) {
}
