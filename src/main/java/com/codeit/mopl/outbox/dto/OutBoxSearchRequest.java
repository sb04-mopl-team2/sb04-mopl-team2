package com.codeit.mopl.outbox.dto;

import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.outbox.entity.AggregateType;
import com.codeit.mopl.outbox.entity.OutBoxSortBy;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record OutBoxSearchRequest(
        @Schema(example = "FOLLOWER_INCREASE")
        EventType eventType,

        @Schema(example = "FOLLOW")
        AggregateType aggregateType,

        @Schema(example = "DEAD")
        OutBoxStatus outBoxStatus,

        @Min(1)
        @Max(1000)
        @Schema(example = "100")
        Integer limit,

        @Schema(example = "3")
        Integer retryCount,

        @Schema(example = "TimeoutException")
        String lastErrorMessage,

        @Schema(example = "ASC")
        SortDirection sortDirection,

        @Schema(example = "createdAt")
        OutBoxSortBy sortBy,

        String cursor,
        UUID idAfter
) {
}
