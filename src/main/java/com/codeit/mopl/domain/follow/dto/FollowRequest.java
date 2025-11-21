package com.codeit.mopl.domain.follow.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FollowRequest(
        @NotNull
        UUID followeeId
) {
}
