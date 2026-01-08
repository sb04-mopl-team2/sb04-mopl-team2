package com.codeit.mopl.outbox.dto;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.OutBoxStatus;

import java.util.UUID;

public record FollowOutBoxEventDto(
        UUID id,
        EventType eventType,
        UUID followId,
        UUID followeeId,
        OutBoxStatus outBoxStatus,
        int retryCount,
        String lastErrorMessage
) {
}
