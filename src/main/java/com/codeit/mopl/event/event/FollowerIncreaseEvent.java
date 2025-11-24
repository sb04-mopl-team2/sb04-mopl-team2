package com.codeit.mopl.event.event;

import java.util.UUID;

public record FollowerIncreaseEvent(
        UUID followeeId
) {
}
