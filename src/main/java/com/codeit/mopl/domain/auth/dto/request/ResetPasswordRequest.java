package com.codeit.mopl.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank
        String email
) {
}
