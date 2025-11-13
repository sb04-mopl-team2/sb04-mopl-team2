package com.codeit.mopl.domain.playlist.dto;

import com.codeit.mopl.domain.notification.entity.SortDirection;

import java.util.List;
import java.util.UUID;

public record CursorResponsePlaylistDto (
        List<PlaylistDto> playlists,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        SortDirection sortDirection
)

{ }
