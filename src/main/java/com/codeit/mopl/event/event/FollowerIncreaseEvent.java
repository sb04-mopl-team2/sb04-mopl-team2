package com.codeit.mopl.event.event;

import java.util.UUID;

public record FollowerIncreaseEvent(
    UUID followId,
    UUID followeeId
) {
}
