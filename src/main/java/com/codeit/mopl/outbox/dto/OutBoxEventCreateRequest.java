package com.codeit.mopl.outbox.dto;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.AggregateType;

import java.util.UUID;

public record OutBoxEventCreateRequest(
        EventType eventType,
        AggregateType aggregateType,
        UUID aggregateId,
        Object domainEvent,
        String eventClassName
) {
}
