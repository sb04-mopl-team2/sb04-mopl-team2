package com.codeit.mopl.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserLockUpdateRequest(
        @NotBlank
        Boolean locked
) {
}
