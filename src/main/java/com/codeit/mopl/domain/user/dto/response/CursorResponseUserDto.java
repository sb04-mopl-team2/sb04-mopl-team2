package com.codeit.mopl.domain.user.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CursorResponseUserDto<T>(
        @NotNull
        List<T> data,
        String nextCursor,
        UUID nextIdAfter,
        @NotNull
        boolean hasNext,
        @NotNull
        Long totalCount,
        @NotBlank
        String sortBy,
        @NotBlank
        String sortDirection
) {
}
