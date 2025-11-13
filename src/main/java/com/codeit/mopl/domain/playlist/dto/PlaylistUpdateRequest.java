package com.codeit.mopl.domain.playlist.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaylistUpdateRequest (
        @NotBlank
        String title,

        @NotBlank
        String description
) {
}
