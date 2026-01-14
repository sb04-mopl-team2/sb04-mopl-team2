package com.codeit.mopl.outbox.dto;

import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.outbox.entity.OutBoxSortBy;

import java.util.List;
import java.util.UUID;

public record CursorResponseOutBoxEventDto(
        List<OutBoxEventDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        OutBoxSortBy sortBy,
        SortDirection sortDirection
) {
}
