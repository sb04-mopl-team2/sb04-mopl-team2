package com.codeit.mopl.domain.watchingsession.entity;

import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.enums.ChangeType;

public record WatchingSessionChange(
    ChangeType type,
    WatchingSessionDto watchingSessionDto,
    Long watcherCount
) {

}
