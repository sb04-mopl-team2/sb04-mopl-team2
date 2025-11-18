package com.codeit.mopl.domain.user.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Slice;

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
        public static <T> CursorResponseUserDto<T> from(Slice<T> slice, String cursor, UUID after, Long totalElements, String sortBy, String sortDirection) {
                return new CursorResponseUserDto<>(
                        slice.getContent(),
                        cursor,
                        after,
                        slice.hasNext(),
                        totalElements,
                        sortBy,
                        sortDirection
                );
        }
}
