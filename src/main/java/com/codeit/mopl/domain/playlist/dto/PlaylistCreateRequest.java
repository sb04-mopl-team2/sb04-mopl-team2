package com.codeit.mopl.domain.playlist.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaylistCreateRequest (
        @NotBlank
        String title,

        @NotBlank
        String description
) { }
