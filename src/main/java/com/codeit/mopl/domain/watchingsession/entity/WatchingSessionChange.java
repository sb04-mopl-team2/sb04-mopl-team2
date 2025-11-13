package com.codeit.mopl.domain.watchingsession.entity;

import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;

public record WatchingSessionChange(
    ChangeType type,
    WatchingSessionDto watchingSessionDto,
    Long watcherCount
) {

}
