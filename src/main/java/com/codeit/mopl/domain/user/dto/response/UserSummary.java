package com.codeit.mopl.domain.user.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserSummary(
        @NotNull
        UUID userId,
        @NotBlank
        String name,
        String profileImageUrl
) {
}
