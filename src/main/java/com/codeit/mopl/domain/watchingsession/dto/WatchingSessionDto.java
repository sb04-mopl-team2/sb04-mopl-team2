package com.codeit.mopl.domain.watchingsession.dto;

import com.codeit.mopl.domain.content.dto.response.ContentSummary;
import com.codeit.mopl.domain.watchingsession.entity.UserSummary;
import java.time.LocalDateTime;
import java.util.UUID;

public record WatchingSessionDto(
    UUID id,
    LocalDateTime createdAt,
    UserSummary userSummary,
    ContentSummary contentSummary
) {

}
