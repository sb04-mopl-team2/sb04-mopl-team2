package com.codeit.mopl.domain.user.dto.request;

import com.codeit.mopl.domain.user.entity.Role;
import jakarta.validation.constraints.NotBlank;

public record UserRoleUpdateRequest(
        @NotBlank
        Role role
) {
}
