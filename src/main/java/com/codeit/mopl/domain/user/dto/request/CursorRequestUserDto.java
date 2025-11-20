package com.codeit.mopl.domain.user.dto.request;

import com.codeit.mopl.domain.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CursorRequestUserDto(
        String emailLike,
        Role roleEqual,
        Boolean isLocked,
        String cursor,
        UUID idAfter,
        @NotNull
        Integer limit,
        @NotBlank
        @Pattern(regexp = "ASCENDING|DESCENDING", flags = Pattern.Flag.CASE_INSENSITIVE)
        String sortDirection,
        @NotBlank
        @Pattern(regexp = "name|email|createdAt|isLocked|role", flags = Pattern.Flag.CASE_INSENSITIVE)
        String sortBy
) {
}
