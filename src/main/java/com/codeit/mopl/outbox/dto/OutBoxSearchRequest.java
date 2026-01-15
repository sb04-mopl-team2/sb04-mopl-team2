package com.codeit.mopl.outbox.dto;

import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.AggregateType;
import com.codeit.mopl.outbox.entity.OutBoxSortBy;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record OutBoxSearchRequest(
        EventType eventType,

        AggregateType aggregateType,

        OutBoxStatus outBoxStatus,

        Integer retryCount,

        String lastErrorMessage,

        SortDirection sortDirection,

        OutBoxSortBy sortBy,

        @Min(1)
        @Max(1000)
        Integer limit,

        String cursor,

        UUID idAfter
) {
}
