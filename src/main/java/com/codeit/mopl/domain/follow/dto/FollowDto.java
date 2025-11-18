package com.codeit.mopl.domain.follow.dto;

import java.util.UUID;

public record FollowDto(
    UUID id,
    UUID followeeId,
    UUID followerId
) {
}
