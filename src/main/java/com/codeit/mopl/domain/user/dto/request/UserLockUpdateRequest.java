package com.codeit.mopl.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserLockUpdateRequest(
        @NotNull
        Boolean locked
) {
}
