package com.codeit.mopl.domain.watchingsession.dto;

import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.base.SortDirection;
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
    String sortBy,
    @NotNull
    SortDirection sortDirection
) {

}
