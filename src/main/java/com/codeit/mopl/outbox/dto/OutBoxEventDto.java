package com.codeit.mopl.outbox.dto;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.AggregateType;
import com.codeit.mopl.outbox.entity.OutBoxStatus;

import java.time.Instant;
import java.util.UUID;

public record OutBoxEventDto(
        UUID id,
        EventType eventType,
        AggregateType aggregateType,
        UUID aggregateId,
        String payload,
        OutBoxStatus outBoxStatus,
        int retryCount,
        String lastErrorMessage,
        Instant createdAt
) {
}
