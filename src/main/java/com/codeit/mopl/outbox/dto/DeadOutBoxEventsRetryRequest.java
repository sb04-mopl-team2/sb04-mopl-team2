package com.codeit.mopl.outbox.dto;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.AggregateType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record DeadOutBoxEventsRetryRequest(

        @Schema(example = "FOLLOWER_INCREASE")
        EventType eventType,

        @Schema(example = "FOLLOW")
        AggregateType aggregateType,

        @Schema(
                example = "2026-01-14",
                description = "이 날짜 이후 생성된 OutBox 이벤트"
        )
        LocalDate createdFrom,

        @Schema(
                example = "2026-01-15",
                description = "이 날짜 이전 생성된 OutBox 이벤트"
        )
        LocalDate createdTo,

        @Min(1)
        @Max(1000)
        @Schema(example = "100")
        Integer limit
) {
}
