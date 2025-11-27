package com.codeit.mopl.event.event;

import java.util.UUID;

public record FollowerDecreaseEvent(
    UUID followId,
    UUID followeeId
) {
}
