package com.codeit.mopl.domain.message.conversation.dto;

import com.codeit.mopl.domain.notification.entity.SortDirection;

import java.util.List;
import java.util.UUID;

public record CursorResponseConversationDto (
        List<ConversationDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        SortDirection sortDirection
) {
}
