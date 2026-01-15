package com.codeit.mopl.outbox.dto;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.AggregateType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record DeadOutBoxEventsRetryRequest(
        EventType eventType,

        AggregateType aggregateType,

        LocalDate createdFrom,

        LocalDate createdTo,

        @Min(1)
        @Max(1000)
        Integer limit
) {
}
