package com.codeit.mopl.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @NotBlank
        String username,
        @NotBlank
        String password
) {
}
