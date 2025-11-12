package com.codeit.mopl.domain.user.dto.response;

import com.codeit.mopl.domain.user.entity.Role;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        @NotBlank
        UUID id,

        @NotBlank
        LocalDateTime createdAt,

        @NotBlank
        String email,

        @NotBlank
        String name,

        String profileImageUrl,

        @NotBlank
        Role role,

        Boolean locked
) {
}
