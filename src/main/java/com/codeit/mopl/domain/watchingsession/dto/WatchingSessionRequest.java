package com.codeit.mopl.domain.watchingsession.dto;

import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WatchingSessionRequest(
    String watcherNameLike,
    String cursor,
    UUID idAfter,
    @Min(1) @Max(100)
    Integer limit,
    @NotNull
    SortDirection sortDirection,
    @NotNull
    SortBy sortBy
) {

}
