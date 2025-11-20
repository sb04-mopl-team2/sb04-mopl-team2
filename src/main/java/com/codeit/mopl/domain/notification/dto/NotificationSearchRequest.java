package com.codeit.mopl.domain.notification.dto;

import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

public record NotificationSearchRequest(
    String cursor,
    UUID idAfter,

    @Min(1)
    int limit,

    @NotNull
    SortDirection sortDirection,

    @NotNull
    SortBy sortBy
) { }
