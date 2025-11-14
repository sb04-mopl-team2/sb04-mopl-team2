package com.codeit.mopl.domain.watchingsession.dto;

import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CursorResponseWatchingSessionDto(
    @NotNull
    List<WatchingSessionDto> data,
    String nextCursor,
    UUID nextIdAfter,
    @NotNull
    Boolean hasNext,
    @NotNull
    Long totalCount,
    @NotNull
    SortBy sortBy,
    @NotNull
    SortDirection sortDirection
) {

}
