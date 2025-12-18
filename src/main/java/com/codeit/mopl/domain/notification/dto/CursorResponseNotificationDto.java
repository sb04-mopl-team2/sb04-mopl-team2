package com.codeit.mopl.domain.notification.dto;

import com.codeit.mopl.domain.base.SortDirection;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CursorResponseNotificationDto(

    @NotNull
    List<NotificationDto> data,

    String nextCursor,

    UUID nextIdAfter,

    @NotNull
    Boolean hasNext,

    @NotNull
    Long totalCount,

    @NotNull
    String sortBy,

    @NotNull
    SortDirection sortDirection

) {}
