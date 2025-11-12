package com.codeit.mopl.domain.auth.dto;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JwtDto(
        @NotNull
        UserDto userDto,
        @NotBlank
        String accessToken
) {
}
