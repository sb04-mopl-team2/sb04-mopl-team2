package com.codeit.mopl.outbox.dto;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.OutBoxStatus;

import java.util.UUID;

public record OutBoxEventDto(
        UUID id,
        EventType eventType,
        UUID eventId,
        String payload,
        OutBoxStatus outBoxStatus,
        int retryCount,
        String lastErrorMessage
) {
}
