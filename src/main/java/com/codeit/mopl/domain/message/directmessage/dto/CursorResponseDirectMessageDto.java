package com.codeit.mopl.domain.message.directmessage.dto;

import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.notification.entity.SortDirection;

import java.util.List;
import java.util.UUID;

public record CursorResponseDirectMessageDto (
        List<DirectMessage> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        SortDirection sortDirection

) {
}
