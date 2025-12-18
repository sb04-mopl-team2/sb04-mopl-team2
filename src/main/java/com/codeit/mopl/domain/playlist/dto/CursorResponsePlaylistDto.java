package com.codeit.mopl.domain.playlist.dto;

import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.domain.base.SortBy;

import java.util.List;
import java.util.UUID;

public record CursorResponsePlaylistDto (
        List<PlaylistDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        SortBy sortBy,
        SortDirection sortDirection
)
{ }
