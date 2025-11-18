package com.codeit.mopl.event.event;

import com.codeit.mopl.domain.follow.dto.FollowDto;

public record FollowerIncreaseEvent(
        FollowDto followDto
) {
}
