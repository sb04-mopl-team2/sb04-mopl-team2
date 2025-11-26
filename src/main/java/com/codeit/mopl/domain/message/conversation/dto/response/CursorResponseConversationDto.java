package com.codeit.mopl.domain.message.conversation.dto.response;

import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.playlist.entity.SortBy;

import java.util.List;
import java.util.UUID;

public record CursorResponseConversationDto (
        List<ConversationDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        SortBy sortBy,
        SortDirection sortDirection
) {
}
