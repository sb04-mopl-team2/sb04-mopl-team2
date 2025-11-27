package com.codeit.mopl.domain.message.directmessage.dto;

import com.codeit.mopl.domain.message.conversation.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;

import java.util.List;
import java.util.UUID;

public record CursorResponseDirectMessageDto (
        List<DirectMessageDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        SortBy sortBy,
        SortDirection sortDirection

) {
}
