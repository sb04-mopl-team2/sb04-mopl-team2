package com.codeit.mopl.domain.user.dto.response;

import java.util.List;
import java.util.UUID;

public record CursorResponseUserDto<T>(
        List<T> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        Long totalCount,
        String sortBy,
        String sortDirection
) {
}
