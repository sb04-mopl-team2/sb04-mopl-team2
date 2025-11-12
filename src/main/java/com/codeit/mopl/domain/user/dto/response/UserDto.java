package com.codeit.mopl.domain.user.dto.response;

import com.codeit.mopl.domain.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        @NotNull
        UUID id,

        @NotNull
        LocalDateTime createdAt,

        @NotBlank
        String email,

        @NotBlank
        String name,

        String profileImageUrl,

        @NotNull
        Role role,

        Boolean locked
) {
}
